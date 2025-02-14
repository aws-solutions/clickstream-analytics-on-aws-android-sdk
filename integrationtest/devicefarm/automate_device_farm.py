# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
import datetime
import os
import random
import string
import time

import boto3
import requests
import yaml
import zipfile
import shutil
import re

# The following script runs a test through Device Farm
client = boto3.client('devicefarm')


def get_config(app_file_path, test_package, project_arn, test_spec_arn, pool_arn):
    return {
        # This is our app under test.
        "appFilePath": app_file_path,
        "projectArn": project_arn,
        # Since we care about the most popular devices, we'll use a curated pool.
        "testSpecArn": test_spec_arn,
        "poolArn": pool_arn,
        "namePrefix": "MyAndroidAppTest",
        # This is our test package. This tutorial won't go into how to make these.
        "testPackage": test_package
    }


def upload_and_test_android(app_file_path, test_package, project_arn, test_spec_arn, pool_arn):
    config = get_config(app_file_path, test_package, project_arn, test_spec_arn, pool_arn)
    print(config)
    unique = config['namePrefix'] + "-" + (datetime.date.today().isoformat()) + (
        ''.join(random.sample(string.ascii_letters, 8)))
    print(f"The unique identifier for this run is going to be {unique} -- all uploads will be prefixed with this.")

    our_upload_arn = upload_df_file(config, unique, config['appFilePath'], "ANDROID_APP")
    our_test_package_arn = upload_df_file(config, unique, config['testPackage'], 'APPIUM_PYTHON_TEST_PACKAGE')
    print(our_upload_arn, our_test_package_arn)
    # Now that we have those out of the way, we can start the test run...
    response = client.schedule_run(
        projectArn=config["projectArn"],
        appArn=our_upload_arn,
        devicePoolArn=config["poolArn"],
        name=unique,
        test={
            "type": "APPIUM_PYTHON",
            "testSpecArn": config["testSpecArn"],
            "testPackageArn": our_test_package_arn
        }
    )
    run_arn = response['run']['arn']
    start_time = datetime.datetime.now()
    print(f"Run {unique} is scheduled as arn {run_arn} ")

    try:
        while True:
            response = client.get_run(arn=run_arn)
            state = response['run']['status']
            if state == 'COMPLETED' or state == 'ERRORED':
                break
            else:
                print(f" Run {unique} in state {state}, total time " + str(datetime.datetime.now() - start_time))
                time.sleep(10)
    except Exception as e:
        # If something goes wrong in this process, we stop the run and exit.
        print(e)
        client.stop_run(arn=run_arn)
        exit(1)
    print(f"Tests finished in state {state} after " + str(datetime.datetime.now() - start_time))
    # now, we pull all the logs.
    jobs_response = client.list_jobs(arn=run_arn)
    # Save the output somewhere. We're using the unique value, but you could use something else
    save_path = os.path.join(os.getcwd(), unique)
    os.mkdir(save_path)
    # Save the last run information
    logcat_paths = download_artifacts(jobs_response, save_path)
    # done
    save_logcat_path(logcat_paths)
    print("Finished")


def upload_df_file(config, unique, filename, type_, mime='application/octet-stream'):
    response = client.create_upload(projectArn=config['projectArn'],
                                    name=unique + "_" + os.path.basename(filename),
                                    type=type_,
                                    contentType=mime
                                    )
    # Get the upload ARN, which we'll return later.
    upload_arn = response['upload']['arn']
    # We're going to extract the URL of the upload and use Requests to upload it
    upload_url = response['upload']['url']
    with open(filename, 'rb') as file_stream:
        print(f"Uploading {filename} to Device Farm as {response['upload']['name']}... ", end='')
        put_req = requests.put(upload_url, data=file_stream, headers={"content-type": mime})
        print(' done')
        if not put_req.ok:
            raise Exception("Couldn't upload, requests said we're not ok. Requests says: " + put_req.reason)
    started = datetime.datetime.now()
    while True:
        print(f"Upload of {filename} in state {response['upload']['status']} after " + str(
            datetime.datetime.now() - started))
        if response['upload']['status'] == 'FAILED':
            raise Exception("The upload failed processing. DeviceFarm says reason is: \n" + (
                response['upload']['message'] if 'message' in response['upload'] else response['upload']['metadata']))
        if response['upload']['status'] == 'SUCCEEDED':
            break
        time.sleep(5)
        response = client.get_upload(arn=upload_arn)
    print("")
    return upload_arn


def download_artifacts(jobs_response, save_path):
    logcat_paths = []
    for job in jobs_response['jobs']:
        # Make a directory for our information
        job_name = job['name']
        os.makedirs(os.path.join(save_path, job_name), exist_ok=True)
        # Get each suite within the job
        suites = client.list_suites(arn=job['arn'])['suites']
        for suite in suites:
            if suite['name'] == 'Tests Suite':
                for test in client.list_tests(arn=suite['arn'])['tests']:
                    # Get the artifacts
                    for artifact_type in ['FILE', 'SCREENSHOT', 'LOG']:
                        artifacts = client.list_artifacts(
                            type=artifact_type,
                            arn=test['arn']
                        )['artifacts']
                        is_valid_test = True
                        for artifact in artifacts:
                            path_to = os.path.join(save_path, job_name)
                            os.makedirs(path_to, exist_ok=True)
                            filename = artifact['type'] + "_" + artifact['name'] + "." + artifact['extension']
                            if str(filename).endswith(".logcat") or str(filename).endswith(".zip"):
                                artifact_save_path = os.path.join(path_to, filename)
                                if is_valid_test and str(filename).endswith(".logcat"):
                                    logcat_paths.append(artifact_save_path)
                                print("Downloading " + artifact_save_path)
                                with open(artifact_save_path, 'wb') as fn, \
                                        requests.get(artifact['url'], allow_redirects=True) as request:
                                    fn.write(request.content)
                                if str(filename).endswith(".zip"):
                                    is_valid_test = unzip_and_copy(artifact_save_path)

    return logcat_paths


def save_logcat_path(logcat_paths):
    with open('path.yaml', 'w') as file:
        yaml.dump(logcat_paths, file, default_flow_style=False)
        print("Logcat paths saved successful")


def unzip_and_copy(zip_path):
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(os.path.dirname(zip_path))
    origin_path = os.path.dirname(zip_path) + "/Host_Machine_Files/$DEVICEFARM_LOG_DIR/junitreport.xml"
    device_name = os.path.basename(os.path.dirname(zip_path))
    rename_path = os.path.dirname(origin_path) + "/" + device_name + " appium junitreport.xml"
    os.rename(origin_path, rename_path)
    report_path = os.path.dirname(os.path.dirname(os.path.dirname(zip_path))) + "/report/"
    os.makedirs(report_path, exist_ok=True)
    result_path = shutil.copy(rename_path, report_path)

    # rename test name
    with open(result_path, 'r', encoding='utf-8') as file:
        content = file.read()
    modified_content = re.sub(r'\bTestShopping\b', "AppiumTest " + device_name, content)
    with open(result_path, 'w', encoding='utf-8') as file:
        file.write(modified_content)
    if content.__contains__("skipped=\"2\""):
        return False
    return True
