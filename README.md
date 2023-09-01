# AWS Solution Clickstream Analytics SDK for Android

[![Maven Central](https://img.shields.io/maven-central/v/software.aws.solution/clickstream.svg)](https://search.maven.org/artifact/software.aws.solution/clickstream)

## Introduction

Clickstream Android SDK can help you easily collect and report in-app events from Android devices to AWS. This SDK is part of an AWS solution - [Clickstream Analytics on AWS](https://github.com/awslabs/clickstream-analytics-on-aws), which provisions data pipeline to ingest and process event data into AWS services such as S3, Redshift.

The SDK relies on the Amplify for Android SDK Core Library and is developed according to the Amplify Android SDK plug-in specification. In addition, we've added features that automatically collect common user events and attributes (e.g., screen view, first open) to simplify data collection for users.

### Platform Support

The Clickstream SDK supports Android API level 16 (Android 4.1) and above.

## Integrate SDK

**1.Include SDK**

Add the following dependency to your `app` module's `build.gradle` file.

```groovy
dependencies {
    implementation 'software.aws.solution:clickstream:0.7.0'
}
```

then sync your project, if you have problem to build your app, please check [troubleshooting](#Troubleshooting)

**2.Parameter configuration**

Find the res directory under your  `project/app/src/main` , and manually create a raw folder in the res directory. 

![](images/raw_folder.png)

Download your `amplifyconfiguration.json` file from your clickstream control plane, and paste it to raw folder, the json file will be as following:

```json
{
  "analytics": {
    "plugins": {
      "awsClickstreamPlugin": {
        "appId": "appId",
        "endpoint": "https://example.com/collect",
        "isCompressEvents": true,
        "autoFlushEventsInterval": 10000,
        "isTrackAppExceptionEvents": false
      }
    }
  }
}
```

Your `appId` and `endpoint` are already set up in it, here's an explanation of each property:

- **appId**: the app id of your project in control plane.
- **endpoint**: the endpoint url you will upload the event to AWS server.
- **isCompressEvents**: whether to compress event content when uploading events, default is `true`
- **autoFlushEventsInterval**: event sending interval, the default is `10s`
- **isTrackAppExceptionEvents**: whether auto track exception event in app, default is `false`

**3.Initialize the SDK**

Please Initialize the SDK in the Application `onCreate()` method.

```java
import software.aws.solution.clickstream.ClickstreamAnalytics;

public void onCreate() {
    super.onCreate();

    try{
        ClickstreamAnalytics.init(getApplicationContext());
        Log.i("MyApp", "Initialized ClickstreamAnalytics");
    } catch (AmplifyException error){
        Log.e("MyApp", "Could not initialize ClickstreamAnalytics", error);
    } 
}
```

**4.Config the SDK**

After initial the SDK we can use the following code to custom configure it.

```java
import software.aws.solution.clickstream.ClickstreamAnalytics;

// config the SDK after initialize.
ClickstreamAnalytics.getClickStreamConfiguration()
            .withAppId("appId")
            .withEndpoint("https://example.com/collect")
            .withAuthCookie("your authentication cookie")
            .withSendEventsInterval(10000)
            .withSessionTimeoutDuration(1800000)
            .withTrackScreenViewEvents(false)
            .withTrackUserEngagementEvents(false)
            .withTrackAppExceptionEvents(false)
            .withLogEvents(true)
            .withCustomDns(CustomOkhttpDns.getInstance())
            .withCompressEvents(true);
```

> note: this configuration will override the default configuration in `amplifyconfiguration.json` file.

### Start using

Now that you've integrated the SDK, let's start using it in your app.

#### Record event

Add the following code where you need to report an event.

```java
import software.aws.solution.clickstream.ClickstreamAnalytics;
import software.aws.solution.clickstream.ClickstreamEvent;

ClickstreamEvent event = ClickstreamEvent.builder()
    .name("PasswordReset")
    .add("Channel", "SMS")
    .add("Successful", true)
    .add("ProcessDuration", 78.2)
    .add("UserAge", 20)
    .build();
ClickstreamAnalytics.recordEvent(event);

// for record an event directly
ClickstreamAnalytics.recordEvent("button_click");
```

#### Add global attribute

```java
import software.aws.solution.clickstream.ClickstreamAttribute;
import software.aws.solution.clickstream.ClickstreamAnalytics;

ClickstreamAttribute globalAttribute = ClickstreamAttribute.builder()
    .add("channel", "HUAWEI")
    .add("level", 5.1)
    .add("class", 6)
    .add("isOpenNotification", true)
    .build();
ClickstreamAnalytics.addGlobalAttributes(globalAttribute);

// for delete an global attribute
ClickstreamAnalytics.deleteGlobalAttributes("level");
```

#### Login and logout

```java
import software.aws.solution.clickstream.ClickstreamAnalytics;

// when user login success.
ClickstreamAnalytics.setUserId("UserId");

// when user logout
ClickstreamAnalytics.setUserId(null);
```

When we log into another user, we will clear the before user's user attributes, after `setUserId()` you need add your user's attribute.

#### Add user attribute

```java
import software.aws.solution.clickstream.ClickstreamAnalytcs;
import software.aws.solution.clickstream.ClickstreamUserAttribute;

ClickstreamUserAttribute clickstreamUserAttribute = ClickstreamUserAttribute.builder()
    .add("_user_age", 21)
    .add("_user_name", "carl")
    .build();
ClickstreamAnalytics.addUserAttributes(clickstreamUserAttribute);
```

Current login user‘s attributes will be cached in disk, so the next time app launch you don't need to set all user's attribute again, of course you can update the current user's attribute when it changes.

#### Log the event json in debug mode

```java
import software.aws.solution.clickstream.ClickstreamAnalytics;

// log the event in debug mode.
ClickstreamAnalytics.getClickStreamConfiguration()
            .withLogEvents(BuildConfig.DEBUG);
```

after config `.withLogEvents(true)` and when you record an event, you can see the event json at your AndroidStudio **Logcat** by filter `EventRecorder`.

#### Config custom DNS

```java
import software.aws.solution.clickstream.ClickstreamAnalytics;

// config custom dns.
ClickstreamAnalytics.getClickStreamConfiguration()
            .withCustomDns(CustomOkhttpDns.getInstance());
```

If you want to use custom DNS for network request, you can create your `CustomOkhttpDns` which implementaion `okhttp3.Dns`, then config `.withCustomDns(CustomOkhttpDns.getInstance())` to make it works.

#### Send event immediately

```java
// for send event immediately.
ClickstreamAnalytics.flushEvent();
```

## How to build locally

open an terminal window, at the root project folder to execute:

```shell
./gradlew build -p clickstream
```

## Troubleshooting

#### Problem: Duplicate class for kotlin-stdlib:1.8.x

If your project use kotlin 1.7 or lower, you may have this problem, you can choose one of  the following three ways to fix it.

1. Upgrade your kotlin version to 1.8

2. Add bom library to your app module's `build.gradle` file, it will align the kotlin version with 1.8
   
   ```kotlin
   implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.10"))
   ```

3. add `resolutionStrategy` to your app module's  `build.gradle` file to force specifying your kotlin version to build.
   
   ```kotlin
   configurations.all {
       resolutionStrategy {
           force("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
       }
   }
   ```

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the [Apache 2.0 License](./LICENSE).