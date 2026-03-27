{
  description = "Swing Editor Dev Environment (Java 17 + Gradle 8.14.4)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      jdk = pkgs.jdk17;
    in {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = [
          jdk
          pkgs.gradle_8
        ];

        shellHook = ''
          export JAVA_HOME=${jdk}
          export PATH=$JAVA_HOME/bin:$PATH

          echo "Java:"
          java -version
          echo "Gradle:"
          gradle -v
        '';
      };
    };
}