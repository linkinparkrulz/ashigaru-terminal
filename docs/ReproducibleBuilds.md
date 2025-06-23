# Reproducible Builds

The purpose of this guide is to aid you in confirming the [official release of Ashigaru Terminal Linux Standalone Binary](http://ashicodepbnpvslzsl2bz7l2pwrjvajgumgac423pp3y2deprbnzz7id.onion/Ashigaru/Ashigaru-Terminal/releases) file can be independently reproduced, therefore confirming the Ashigaru Open Source Project has released a binary built **only** using source code from the [Ashigaru-Terminal repository](http://ashicodepbnpvslzsl2bz7l2pwrjvajgumgac423pp3y2deprbnzz7id.onion/Ashigaru/Ashigaru-Terminal).

The general goal is for you to independently build your own unsigned production binary using the [Ashigaru-Terminal source code](http://ashicodepbnpvslzsl2bz7l2pwrjvajgumgac423pp3y2deprbnzz7id.onion/Ashigaru/Ashigaru-Terminal).

## Building reproducible builds

### Version information

These instructions are valid for officially released Ashigaru Terminal APKs from version 1.0.0

### Environment

Intel x86_64 architecture. Using other architecture may yield different results.

### Download image
`sudo docker pull eclipse-temurin:21.0.7_6-jdk`

### Start the container
`sudo docker run -it --name ashigaru eclipse-temurin:21.0.7_6-jdk bash`

### Exit the container
`exit` (or Ctrl + D)

### Download source code
Use `torsocks wget` with Source Code (ZIP) for the latest release tag

### Copy the project to it
`docker cp path/to/Ashigaru-terminal-development ashigaru:/root`

### Start the container again and go inside it
`docker start -ai ashigaru`

### Install dependencies and compile the project
`apt update && apt install -y rpm fakeroot`

`cd /root/Ashigaru-terminal-development`

`./gradlew -Djava.awt.headless=true clean jpackage`

`./gradlew packageTarDistribution`

### Check hash
`sha256sum build/jpackage/ashigaru-terminal-1.0.0-x86_64.tar.gz`

### Compare result
Compare the SHA256 output of the binary you have created against the binary file officially released by the Ashigaru Open Source Project.