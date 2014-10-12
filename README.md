1. Run with

In test mode
java -jar target/cloodle-0.1.0-SNAPSHOT-standalone.jar

In production mode
java -DMODE=PROD -jar target/cloodle-0.1.0-SNAPSHOT-standalone.jar


To implement for initial usable version
---------------------------------------

* Show stoppers
  * ~~Sliders are not linked to the state~~
  * ~~Moving the slider creates new options/value maps rather than updating existing ones~~
  * Saving a vote doesn't work
    * ~~Generate option ids server side~~
    * ~~Remove client-side option id generation~~
    * ~~Return the generated option ids (maybe the whole event map? from the server-side after saving)~~
    * After saving a new event, mongo id is not in the state -> voting fails
    * ~~Saving a vote by sending the event id and the participant name + selections, not the whole thing~~
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





