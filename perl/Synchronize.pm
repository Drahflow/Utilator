package Synchronize;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(dbh create_gid iso_date PUBLICATION_LOCAL PUBLICATION_PRIVATE PUBLICATION_TRANSPARENCY PUBLICATION_OFFER);

use strict;
use warnings;
use encoding 'utf-8';

use Global qw(/./);

sub sync {
  my ($json, $verbose, $startDate) = (@_);

  dbh()->do("BEGIN TRANSACTION");

  my %inTasks = map { ($_->{'gid'}, $_) } @{$json->{'task'}};

  $startDate = iso_date(time - 60 * 86400) unless defined $startDate;

  my @tasks = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $startDate)};
  SELECT * FROM task WHERE last_edit IS NULL OR last_edit > ?
EOSQL

  my %utility = ();
  my %likelyhood_time = ();
  my %external = ();

  my @utility = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $startDate)};
  SELECT u.distribution, u.task FROM task_utility u JOIN task t ON t.id = u.task WHERE t.last_edit IS NULL OR t.last_edit > ?
EOSQL
  foreach my $u (@utility) {
    push @{$utility{$u->{'task'}}}, $u->{'distribution'};
  }

  my @likelyhood_time = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $startDate)};
  SELECT l.distribution, l.task FROM task_likelyhood_time l JOIN task t ON t.id = l.task WHERE t.last_edit IS NULL OR t.last_edit > ?
EOSQL
  foreach my $l (@likelyhood_time) {
    push @{$likelyhood_time{$l->{'task'}}}, $l->{'distribution'};
  }

  my @external = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $startDate)};
  SELECT e.external, e.task FROM task_external e JOIN task t ON t.id = e.task WHERE t.last_edit IS NULL OR t.last_edit > ?
EOSQL
  foreach my $e (@external) {
    push @{$external{$e->{'task'}}}, $e->{'distribution'};
  }

  foreach my $task (@tasks) {
    print "Loading $task->{'gid'}...\n" if $verbose;

    $task->{'utility'} = $utility{$task->{'id'}}
      if($utility{$task->{'id'}});
    $task->{'likelyhood_time'} = $likelyhood_time{$task->{'id'}}
      if($likelyhood_time{$task->{'id'}});
    $task->{'external'} = $external{$task->{'id'}}
      if($external{$task->{'id'}});
  }

  my %tasks = map { ($_->{'gid'}, $_) } @tasks;

  my %syncedTasks;
  my %localChangedTasks;
  my %remoteChangedTasks;

  foreach my $gid (keys %{{ map { ($_, 1) } (keys %tasks), (keys %inTasks) }}) {
    if(not exists $tasks{$gid}) {
      $syncedTasks{$gid} = $inTasks{$gid};
      $localChangedTasks{$gid} = $inTasks{$gid};
    } elsif(not exists $inTasks{$gid}) {
      $syncedTasks{$gid} = $tasks{$gid};
      $remoteChangedTasks{$gid} = $tasks{$gid};
    } elsif($inTasks{$gid}->{'last_edit'} gt $tasks{$gid}->{'last_edit'}) {
      $syncedTasks{$gid} = $inTasks{$gid};
      $localChangedTasks{$gid} = $inTasks{$gid};
    } else {
      $syncedTasks{$gid} = $tasks{$gid};
      $remoteChangedTasks{$gid} = $inTasks{$gid};
    }
  }

  foreach my $task (values %localChangedTasks) {
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

    foreach my $entry (@{$task->{'utility'} or []}) {
      dbh()->do(<<EOSQL, {}, $task->{'gid'}, $entry);
INSERT INTO task_utility (task, distribution) VALUES((SELECT id FROM task WHERE gid = ?), ?)
EOSQL
    }

    dbh()->do(<<EOSQL, {}, $task->{'gid'});
DELETE FROM task_likelyhood_time WHERE task = (SELECT id FROM task WHERE gid = ?)
EOSQL

    foreach my $entry (@{$task->{'likelyhood_time'} or []}) {
      dbh()->do(<<EOSQL, {}, $task->{'gid'}, $entry);
INSERT INTO task_likelyhood_time (task, distribution) VALUES((SELECT id FROM task WHERE gid = ?), ?)
EOSQL
    }

    dbh()->do(<<EOSQL, {}, $task->{'gid'});
DELETE FROM task_external WHERE task = (SELECT id FROM task WHERE gid = ?)
EOSQL

    foreach my $entry (@{$task->{'external'} or []}) {
      dbh()->do(<<EOSQL, {}, $task->{'gid'}, $entry);
INSERT INTO task_external (task, external) VALUES((SELECT id FROM task WHERE gid = ?), ?)
EOSQL
    }
  }

  dbh()->do("COMMIT");
  dbh()->disconnect();

  my $result = {
    'version' => 1,
    'task' => [values %remoteChangedTasks],
  };
}

1;
