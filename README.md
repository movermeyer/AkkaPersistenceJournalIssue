# AkkaPersistenceJournalIssue
A piece of sample code that illustrates (LevelDB?) leaking journal files.

##Usage
Clone this repo

    git clone https://github.com/movermeyer/AkkaPersistenceJournalIssue.git

Within the cloned repo, run:
  
    sbt run
  
Observe the program for some time (several minutes. It's usually obvious after about 10).

Notice that the number of journal files in the journal/ directory has increased, and so has the disk space used. I would have expected that the number of journal files wouldn't increase, as I am calling 'deleteMessages' every time a snapshot succeeds.

##Long running tests

There is also a small bash script included (watch_journal_files.sh) which can be run in the root directory of the cloned repo. It outputs the date, # of journal files, and disk space used by the journal files every two seconds. It outputs to a CSV file, and to stdout. This can be useful if you want to run the test for a long time and want to collect the information automatically.
