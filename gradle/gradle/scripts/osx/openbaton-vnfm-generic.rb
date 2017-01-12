class OpenbatonVnfmGeneric < Formula
  desc "Formula of Open Baton VNFM Generic"
  homepage "http://www.openbaton.org"
  url "https://codeload.github.com/openbaton/generic-vnfm/legacy.tar.gz/develop"
  version "3.1.1-SNAPSHOT"
  # sha256 "7a6fde8dd84f09b935b75d85cb1e58327007f677ebc05f8b31d8e46132725fc5"

  depends_on :java => "1.7+"
  depends_on "gradle"
  depends_on "rabbitmq"

  def install
    system "./generic-vnfm.sh", "compile"
    system "./gradlew", "installDist"
    # Change external path with /usr/local/etc instead /etc (due to brew internal directories)
    inreplace "build/install/generic-vnfm/bin/openbaton-vnfm-generic", "CONFIG_FILE=/config/file/path", "CONFIG_FILE=#{etc}/openbaton/vnfm/generic/application.properties"
    # Change application path
    inreplace "build/install/generic-vnfm/bin/openbaton-vnfm-generic", /APP_HOME="`pwd -P`"/, %(APP_HOME="#{libexec}")

    # Copy the openbaton.properties in the right location
    openbaton_vnfm_generic_properties_path = etc+"openbaton/vnfm/generic"
    openbaton_vnfm_generic_properties_path.mkpath
    openbaton_vnfm_generic_properties_path.install "src/main/resources/application.properties" , "src/main/resources/user-data.sh" , "src/main/resources/conf.properties"

    # Remove Windows file
    rm_f Dir["build/install/generic-vnfm/bin/*.bat"]

    libexec.install Dir["build/install/generic-vnfm/*"]
    bin.install_symlink Dir["#{libexec}/bin/openbaton-vnfm-generic"]
  end
  test do
    system "false"
  end
end
