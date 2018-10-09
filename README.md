# LevelDB Journal Issue
A piece of sample code that illustrates LevelDB leaking journal files, using the akka-persistence's new [PersistentActors](http://doc.akka.io/docs/akka/snapshot/scala/persistence.html)

I believe that it is related to the following issue,

* [Mailing List](https://groups.google.com/forum/#!searchin/akka-user/akka-persistentchannel-does-not-delete-message-from-journal-upon-confirm/akka-user/_d9gpIJyKe0/B6Ie_axaFQMJ)
* [Assembla](https://www.assembla.com/spaces/akka/tickets/3962)
* [GitHub](https://github.com/akka/akka/issues/13962)

as it has the same symptoms. Indeed, [this other repo](https://github.com/manasdebashiskar/akkapersistenceExample) seems to produce the same issue, except it uses the deprecated PersistentChannel.

Note that this has nothing to do with the way akka-persistence uses leveldb, but seems to be an unstated limitation of leveldb with regards to large value sizes.

[It looks like others have hit this issue as well.](https://groups.google.com/forum/#!msg/leveldb/yL6h1mAOc20/vLU64RylIdMJ)

## Usage

Clone this repo

    git clone https://github.com/movermeyer/AkkaPersistenceJournalIssue.git

Within the cloned repo, run:
  
    sbt run
  
Observe the program for some time (several minutes. It's usually obvious after about 10).

Notice that the number of journal files in the journal/ directory has increased, and so has the disk space used. I would have expected that the number of journal files wouldn't increase, as I am calling 'deleteMessages' every time a snapshot succeeds.

## Example

Here is a chart of the disk usage used during a 13 minute test (Each of the ticks on the X-axis represent 2 seconds).

![alt tag](https://i.imgur.com/y7pyRhb.png)

Note that the journal space used is nearly 700 MiB!

## Long running tests

There is also a small bash script included (watch_journal_files.sh) which can be run in the root directory of the cloned repo. It outputs the date, # of journal files, and disk space used by the journal files every two seconds. It outputs to a CSV file, and to stdout. This can be useful if you want to run the test for a long time and want to collect the information automatically.
