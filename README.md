What is this and why do I want this?
====================================

This is a (prototype of) a system to compute a better scheduling for personal tasks.
It is meant to partially replace your very personal opinion of what is important and
when to procrastinate it.

It is mainly based on estimating the current "utility per time" you can gain from
performing an action. If you have never heard of utiliarism before, maybe look it
up on wikipedia on somewhere. Anyway, you'll need a way to give a linear goodness
value to every single task, or more precisely, the amount of goodness difference
of the world if only the task would have magically been done. You can go into
infinite discussions about how to do this, but this is out-of-scope here.
Anyway, the software (and many other people) call this goodness "utility".

Installation + Usage
====================

The current system consists of loosely coupled parts, all of which somehow contribute
to a central database on each computer. Integration with the window manager,
mail client, etc. is done on a hack-by-hack basis, so the installation procedure
depends heavily on your personal system setup.

Script-Collection
-----------------

The /perl branch of the repository is where the stuff for normal PCs lives. Put it
somewhere you can easily access it from the commandline. It's probably easiest to
understand the system by starting from the scripts an only later integrate them
with the rest of the system.

### init

 init

Initializes the local database. Execute this once.

### create

 create 'Understand the tutorial' 2.3 30m
        1111111111111111111111111 222 333
 
 1: title of the new task
 2: the estimated utility of the task completion
 3: the amount of time you will need to complete the task
    5s: five seconds
    5m: five minutes
    5h: five hours
    5d: five days (not 120h, as you'll not work 24h days)
    5w: five weeks (not 35d, as you'll not work 7d weeks)

This command will spew forth the GUID of the new task.

#### Installation

It makes sense to configure various software so easy the taskification of their
current context.
So far I only configured
https://addons.mozilla.org/en-US/thunderbird/addon/passtoscript/
together with the create-frommail script to get me a mail-to-task button.

### query

 query

This will just dump an ordered list of tasks

 query 3 5m
       1 22
 
 1: the maximal number of tasks to show (highest utility per time first)
 2: the maximal time of the task, useful if you need to go soon

#### Installation

To make this really useful, it makes sense to display the result of
 query 1
somewhere on the screen continually (or at least after a single hotkey). Then if
your attention strays, you can always look up what you should be working on. It
will become a habit.

As I use wmii, my wmiirc includes this snippet
 # Status Bar Info
 status() {
         echo -n $(cd /home/drahflow/utilator/perl && query 1 | iconv -f utf-8 -t iso8859-15 | cut -c 1-100)
 }

### dump

 dump CB5BE551-C5C3-4884-B3D7-CC4FC761061C
      111111111111111111111111111111111111
 
 1: task specification
    XXXXXXXX-XXXX-4XXX-XXXX-XXXXXXXXXXXX: the task identified by this GUID
    c: the currently most important task (i.e. you will nearly always use 'dump c')
    l: the task last edited or created (useful if you created a task but it did not become highest priority)
    /regex/: the single task matching regex, errors out if multiple tasks are matched
    /regex/a: as /regex/, but also match against completed tasks (if you accidentally marked a task complete)

This dumps the current task status.

### edit

Task adressing and time intervals are specifed just as before.

 edit CB5BE551-C5C3-4884-B3D7-CC4FC761061C done

Marks the specified task done.

 edit c status 50

Marks the current task 50% done.

 edit l status 0

Marks the last edited task 0% done (if you accidentally completed it).

 edit c wait 5m

Marks the task as impossible-to-act-upon for 5 minutes. Use this only if the external world makes it
impossible for you to complete the task, say because the task says 'buy butter', but it's 4 AM. Time
intervals are not corrected for non-work-time, i.e. 1d is 24h in this command.

 edit c wait_until 2030-01-01T05:00:00

Marks the task as impossible-to-act-upon until the specified date.

 edit c est 5h

Set a new time estimate.

 edit c title 'Make up a better title for this task.'

Set a new title.

 edit c util 0.5

Set a new utility.

#### Installation

It makes sense to integrate some of these commands into global hotkeys. I only mapped 'edit c done',
again via wmii:
 Key $MODKEY-Shift-d # Mark current task done
     (cd ~/utilator/perl && ./edit c done)

### act / act-context

 act c

This script tries to switch your computer into a state most helpful in working on the specified task
(which should always be 'c').

 act-context c

This script tries to classify some of my window contexts on whether they facilitate the execution of
the specified task. Highly wmii specific.

#### Installation

The innards of script really depend on your system configuration. The current version works against
wmii, but other window-managers can probably be scripted somehow as well. Also there should be a
single hotkey calling this script. Then you have a hotkey which essentially says "make me do this".

The same applies to act-context. To use it, you'll probably have to rewrite it (and just keep the
ideas).

### background

This includes various background jobs. Look into it and configure it to call other scripts as
necessary to automatically import external events into your task queue.

### import-*

Various scripts to import from external sources. You'll probably have to edit them to get useful
results.
