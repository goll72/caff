{
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      shell = system:
        let
          lib = nixpkgs.lib;
          
          pkgs = import nixpkgs {
            inherit system;

            config.allowUnfree = true;
            config.android_sdk.accept_license = true;
          };

          pkgs-x86_64-linux = import nixpkgs { system = "x86_64-linux"; };

          jdk = pkgs.jdk17;

          # NOTE: gradle's android plugin v8.13 needs build-tools 35.0.0
          #
          # NOTE: if you're on aarch64, you will need to add
          # the following to `configuration.nix` in order to
          # be able to run `aapt2` from build-tools:
          #
          # ```nix
          # boot.binfmt.emulatedSystems = [ "x86_64-linux" ];
          # ```
          mainBuildToolsVersion = "35.0.0";

          composition = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "36" ];
            buildToolsVersions = [ mainBuildToolsVersion ];
  
            systemImageTypes = [];
            abiVersions = [ "arm64-v8a" ];
  
            includeNDK = true;
            includeCmake = false;
            includeEmulator = false;
            includeSystemImages = false;
            includeSources = false;
            useGoogleAPIs = false;
          };

          androidHome = "${composition.androidsdk}/libexec/android-sdk";
          gradleProperties = pkgs.writeText "gradle.properties" ''
            org.gradle.configuration-cache=true
            org.gradle.parallel=true
            org.gradle.caching=true
            org.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m

            android.useAndroidX=true

            org.gradle.java.installations.paths=${jdk}/lib/openjdk
            android.aapt2FromMavenOverride=${androidHome}/build-tools/${mainBuildToolsVersion}/aapt2
          '';
        in
          pkgs.mkShell {
            packages = [
              # These aren't actually needed to build the project
              pkgs.helix
              pkgs.lemminx
              pkgs.jdt-language-server
              pkgs.kotlin-language-server
              pkgs.fdroidserver

              pkgs.gradle
              composition.androidsdk
            ];

            shellHook = ''
              export ANDROID_SDK_ROOT="${androidHome}"
              export ANDROID_NDK_ROOT="${androidHome}/ndk-bundle"

              export JAVA_HOME="${jdk}/lib/openjdk"

              ln -sf ${gradleProperties} gradle.properties
              git update-index --skip-worktree gradle.properties

              set -o allexport
              [ -f .env ] && . .env
              set +o allexport

              shopt -s globstar
            '' + lib.optionalString (system != "x86_64-linux") ''

              export QEMU_LD_PREFIX="${pkgs-x86_64-linux.glibc}"
            '';
          };
    in
      nixpkgs.lib.mergeAttrsList
        (nixpkgs.lib.forEach [ "x86_64-linux" "aarch64-linux" ] (system: {
          devShell."${system}" = shell system;
        }));
}
