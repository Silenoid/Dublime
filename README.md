# Dublime

This tool is born with the main purpose of recording counter-audio files contained in a directory, like Source Engine audio files.

## Build

The build uses Gradle. Java and Gradle versions are pinned in `mise.toml`; run `mise install` once to provision them.

```shell script
gradle clean shadowJar
```

This triggers the [Shadow](https://gradleup.github.io/shadow/) plugin, producing a runnable fat Jar at
`build/libs/dublime-1.1.0-all.jar` that contains every dependency needed.

Run it with:

```shell script
java -jar build/libs/dublime-1.1.0-all.jar
```

## Problems

### The UI not instantiating Form's elements
**Problem**: The app will run smoothly in the IDE but when packed in Jar it will throw an NPE on
 uninitiated GUI elements in the Form.

**Reason**: When IntelliJ runs the app, it manages the form instantiation thanks to a tool embedded in the IDE. This 
helps triggering the `MainForm.$$$setupUI$$$` process in order to get every instance injected in the JPanel class of
the respective form.
Once you have the Jar exported, this tool is not present anymore and would otherwise need to be provided in another way,
such as a build-tool plugin (e.g. the legacy `ideauidesigner-maven-plugin`). These artifacts seem **not mature nor supported**.

**Solution**: I decided to let the IntelliJ UI Designer generate the sourcecode for those classes:

> Project > Settings > Editor > GUI Designer > Generate GUI into > Java Source Code

In this way, target will be populated with those classes that the UI setup process is expecting when the 
runtime will be detached from IntelliJ IDEA (and the GUI Designer tool).