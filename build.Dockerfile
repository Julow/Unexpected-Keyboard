FROM thyrlian/android-sdk

RUN apt install -y make

RUN sdkmanager --update
RUN sdkmanager "build-tools;27.0.1"
RUN sdkmanager "platforms;android-27"

ENV ANDROID_BUILD_TOOLS "${ANDROID_HOME}/build-tools/27.0.1"
ENV ANDROID_PLATFORM "${ANDROID_HOME}/platforms/android-27"

ENV PATH "${PATH}:${ANDROID_BUILD_TOOLS}"

RUN mkdir /app
WORKDIR /app

CMD make
