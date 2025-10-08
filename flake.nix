{
  description = "NoAnalyt's configuration";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.05";
  };

  outputs =
    {
      self,
      nixpkgs,
      ...
    }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
        config.android_sdk.accept_license = true;
      };
      inherit (pkgs) stdenv lib;

      mkAndroidSdk = localDevelopment: rec {
        android = {
          versions = {
            platformTools = "35.0.1";
            buildTools = [
              "35.0.0"
            ];
            cmdLine = "9.0";
            emulator = "32.1.15";
          };

          platforms = [ "35" ];
          abis = [ "x86" "x86_64" ]; # "armeabi-v7a" "arm64-v8a"
          extras = [ "extras;google;gcm" ];
        };

        sdkArgs = {
          platformToolsVersion = android.versions.platformTools;
          buildToolsVersions = android.versions.buildTools;
          includeEmulator = localDevelopment;
          emulatorVersion = android.versions.emulator;
          platformVersions = android.platforms;
          includeSources = localDevelopment;
          includeSystemImages = localDevelopment;
          systemImageTypes = [ "google_apis_playstore" ];
          abiVersions = android.abis;
          cmdLineToolsVersion = android.versions.cmdLine;
          useGoogleAPIs = false;
          extraLicenses = [
            "android-sdk-preview-license"
            "android-googletv-license"
            "android-sdk-arm-dbt-license"
            "google-gdk-license"
            "intel-android-extra-license"
            "intel-android-sysimage-license"
            "mips-android-sysimage-license"
          ];
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages sdkArgs;
        androidSdk = androidComposition.androidsdk;
        androidSdkHome = "${androidSdk}/libexec/android-sdk";
      };

      devAndroidSdk = mkAndroidSdk true;
      ciAndroidSdk = mkAndroidSdk false;

    in
    {
      # Development shell
      devShells.${system}.default = pkgs.mkShell {
        packages =
          (with pkgs; [
            jdk17
            gradle
          ]);

        ANDROID_HOME = "${devAndroidSdk.androidSdkHome}";
        ANDROID_NDK_ROOT = "${devAndroidSdk.androidSdkHome}/ndk-bundle";
        GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${devAndroidSdk.androidSdkHome}/build-tools/${lib.last devAndroidSdk.android.versions.buildTools}/aapt2";
      };

      # Package outputs
      packages.${system} = {
        check = stdenv.mkDerivation {
          name = "noanalyt-check";
          src = ./.;

          buildInputs =
            (with pkgs; [
              jdk17
              gradle
            ]);
          dontConfigure = true;
          dontInstall = true;

          buildPhase = ''
            export HOME=$(mktemp -d)
            export GRADLE_USER_HOME=$HOME/.gradle
            ./gradlew clean :noanalyt-runtime-api:createVersionKt --no-daemon
            ./gradlew test --no-daemon
          '';

          ANDROID_HOME = "${ciAndroidSdk.androidSdkHome}";
          ANDROID_NDK_ROOT = "${ciAndroidSdk.androidSdkHome}/ndk-bundle";
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ciAndroidSdk.androidSdkHome}/build-tools/${lib.last ciAndroidSdk.android.versions.buildTools}/aapt2";
        };
      };

      formatter.${system} = pkgs.nixfmt-tree;
    };
}
