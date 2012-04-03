package Synchronize;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(dbh create_gid iso_date PUBLICATION_LOCAL PUBLICATION_PRIVATE PUBLICATION_TRANSPARENCY PUBLICATION_OFFER);

use strict;
use warnings;
use encoding 'utf-8';

use Global qw(/./);

sub sync($$) {
  my ($json, $verbose) = (@_);

  dbh()->do("BEGIN TRANSACTION");

  my %inTasks = map { ($_->{'gid'}, $_) } @{$json->{'task'}};

  my @tasks = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, iso_date(time - 356 * 86400))};
SELECT * FROM task WHERE last_edit IS NULL OR last_edit > ?
EOSQL

  foreach my $task (@tasks) {
    print "Loading $task->{'gid'}...\n" if $verbose;

    $task->{'utility'} = [map { $_->{'distribution'} } @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $task->{'gid'})}];
SELECT distribution FROM task_utility WHERE task = (SELECT id FROM task WHERE gid = ?)
EOSQL
    $task->{'likelyhood_time'} = [map { $_->{'distribution'} } @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $task->{'gid'})}];
SELECT distribution FROM task_likelyhood_time WHERE task = (SELECT id FROM task WHERE gid = ?)
EOSQL
  }

  my %tasks = map { ($_->{'gid'}, $_) } @tasks;

  my %syncedTasks;

  foreach my $gid (keys %{{ map { ($_, 1) } (keys %tasks), (keys %inTasks) }}) {
    if(not exists $tasks{$gid}) {
      $syncedTasks{$gid} = $inTasks{$gid};
    } elsif(not exists $inTasks{$gid}) {
      $syncedTasks{$gid} = $tasks{$gid};
    } elsif($inTasks{$gid}->{'last_edit'} gt $tasks{$gid}->{'last_edit'}) {
      $syncedTasks{$gid} = $inTasks{$gid};
    } else {
      $syncedTasks{$gid} = $tasks{$gid};
    }
  }

  foreach my $task (values %syncedTasks) {
    print "Writing $task->{'gid'}...\n" if $verbose;

    if(not exists $tasks{$task->{'gid'}}) {
      dbh()->do(<<EOSQL, {}, $task->{'gid'})
INSERT INTO task (gid) VALUES (?)
EOSQL
    }

    dbh()->do(<<EOSQL, {}, map { $task->{$_} } qw(title description author seconds_estimate seconds_taken status closed_at publication last_edit gid));
UPDATE task SET title = ?, description = ?, author = ?, seconds_estimate = ?, seconds_taken = ?, status = ?, closed_at = ?, publication = ?, last_edit = ? WHERE gid = ?
EOSQL

    dbh()->do(<<EOSQL, {}, $task->{'gid'});
DELETE FROM task_utility WHERE task = (SELECT id FROM task WHERE gid = ?)
EOSQL

    foreach my $entry (@{$task->{'utility'}}) {
      dbh()->do(<<EOSQL, {}, $task->{'gid'}, $entry);
INSERT INTO task_utility (task, distribution) VALUES((SELECT id FROM task WHERE gid = ?), ?)
EOSQL
    }

    dbh()->do(<<EOSQL, {}, $task->{'gid'});
DELETE FROM task_likelyhood_time WHERE task = (SELECT id FROM task WHERE gid = ?)
EOSQL

    foreach my $entry (@{$task->{'likelyhood_time'}}) {
      dbh()->do(<<EOSQL, {}, $task->{'gid'}, $entry);
INSERT INTO task_likelyhood_time (task, distribution) VALUES((SELECT id FROM task WHERE gid = ?), ?)
EOSQL
    }
  }

  dbh()->do("COMMIT");
  dbh()->disconnect();

  my $result = {
    'version' => 1,
    'task' => [values %syncedTasks],
  };
}

1;
