1. Run with

In test mode
java -jar target/cloodle-0.1.0-SNAPSHOT-standalone.jar

In production mode
java -DMODE=PROD -jar target/cloodle-0.1.0-SNAPSHOT-standalone.jar


To implement for initial usable version
---------------------------------------
[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/clojurecup2014/cloodle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

* Show stoppers
  * ~~Sliders are not linked to the state~~
  * ~~Moving the slider creates new options/value maps rather than updating existing ones~~
  * ~~Saving a vote doesn't work~~
  * Saved vote is not immediately shown in the UI
  * ~Existing participant components don't show the participant name~
  * Existing participant components are ugly
  * Vote validation (Old selection validation will not work with the new {optionId value} structure)


* Core features
  * Displaying the aggregate voting results for the event

* Misc
  * The Cloodle code could be shorter(?) and nicer. Make sure it never has characters that wouldn't work in the URL
  * Remove the MongoBase urls / keys from the repo, provide from property files or something
  * Replace validateur with Schema

* Usability / Cosmetics
  * The participant name input is unintuitive - confused with the event name input
  * Don't allow submitting with zero options
  * Allow only one option





Future features
---------------

* Allow several questions for an event (e.g. what movie and when)





