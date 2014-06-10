package Query;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(sortedTasks fetch_space_active expectations resolveExpectation);

use strict;
use warnings;

use Global qw(/./);
use Configuration;

use Date::Format;
use Date::Parse;
use Data::Dumper;

my %utility;
my %likelyhood_time;
my %likelyhood_space;

sub resolveTask($) {
  my ($task) = (@_);

  return undef unless defined $task;

  my $now = iso_full_date(time);

  if($task eq 'c') {
    $task = [sortedTasks($now)]->[-1] or die "no current task";
  } elsif($task eq 'l') {
    my $gid = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })->[0];
    SELECT gid FROM task_edit_last
EOSQL

    die "no last-edited task" unless $gid->{'gid'};

    $task = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $gid->{'gid'})->[0] or die "gid not found";
    SELECT * FROM task WHERE gid = ?
EOSQL
  } elsif($task =~ m!^/(.*)/(.*)$!) {
    my $mask = $1;
    my $opts = $2;

    $task = undef;

    my @tasks;

    if($opts =~ /a/) {
      @tasks = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
      SELECT * FROM task
EOSQL
    } else {
      @tasks = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
      SELECT * FROM task WHERE status < 100
EOSQL
    }
    @tasks = grep {
      defined $_->{'title'} and $_->{'title'} =~ m/$mask/ or
      defined $_->{'description'} and $_->{'description'} =~ m/$mask/ or
      defined $_->{'gid'} and $_->{'gid'} =~ m/$mask/
    } @tasks;

    if(@tasks > 1) {
      foreach my $t (@tasks) {
        printf "%s: %s\n", $t->{'gid'}, $t->{'title'};
      }

      die "multiple tasks found";
    } else {
      $task = $tasks[0] or die "no tasks";
    }
  } else {
    $task = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $task)->[0] or die "gid not found";
    SELECT * FROM task WHERE gid = ?
EOSQL
  }

  return $task;
}

sub sortedTasks {
  my ($now, $all) = @_;

  %utility = ();
  %likelyhood_time = ();

  my @utility = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
  SELECT u.distribution, u.task FROM task_utility u JOIN task t ON t.id = u.task WHERE t.status < 100
EOSQL
  foreach my $u (@utility) {
    push @{$utility{$u->{'task'}}}, $u->{'distribution'};
  }

  my @likelyhood_time = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
  SELECT l.distribution, l.task FROM task_likelyhood_time l JOIN task t ON t.id = l.task WHERE t.status < 100
EOSQL
  foreach my $l (@likelyhood_time) {
    push @{$likelyhood_time{$l->{'task'}}}, $l->{'distribution'};
  }

  my @likelyhood_space = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
  SELECT l.distribution, l.task FROM task_likelyhood_space l JOIN task t ON t.id = l.task WHERE t.status < 100
EOSQL
  foreach my $l (@likelyhood_space) {
    push @{$likelyhood_space{$l->{'task'}}}, $l->{'distribution'};
  }

  my @tasks;
  if($all) {
    @tasks = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
    SELECT * FROM task
EOSQL
  } else {
    @tasks = @{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })};
    SELECT * FROM task WHERE status < 100
EOSQL
  }

  @tasks = map { evaluate_utility($now, $_) } @tasks;
  @tasks = sort { $a->{'current_utility'} <=> $b->{'current_utility'} } @tasks;

  return @tasks;
}

sub getTaskUtility($) {
  my ($task) = @_;
  die "call sortedTasks before" unless %utility;

  return $utility{$task->{'id'}};
}

sub getTaskLikelyhoodTime($) {
  my ($task) = @_;
  die "call sortedTasks before" unless %likelyhood_time;

  return $likelyhood_time{$task->{'id'}};
}

sub compileDistributions {
  my ($task) = @_;

  if(exists $utility{$task->{'id'}}) {
    if(@{$utility{$task->{'id'}}} == 1 and not $likelyhood_time{$task->{'id'}}) {
      if($utility{$task->{'id'}}->[0] =~ /.constant:(.*)/) {
        $task->{'compiled_utility'} = $1 * 990 / 1000000;
      }
    }
  }
}

sub evaluate_utility_compiled {
  my ($now, $task) = @_;

  if($task->{'compiled_utility'}) {
    $task->{'current_utility'} = $task->{'compiled_utility'} / $task->{'seconds_estimate'};
    return $task;
  }

  return evaluate_utility($now, $task);
}

sub evaluate_utility {
  my ($now, $task) = @_;

  if($task->{'status'} >= 100) {
    $task->{'current_utility'} = 0;
  } else {
    my $time_estimate;

    if($task->{'status'} > 0) {
      $time_estimate = $task->{'seconds_estimate'} * (100 - $task->{'status'}) / 100;
    } else {
      $time_estimate = $task->{'seconds_estimate'};
    }

    my $utility = evaluate_time_distribution($now, 0, ($utility{$task->{'id'}} or []));
    my $likelyhood_time = evaluate_time_distribution($now, 990, ($likelyhood_time{$task->{'id'}} or []));
    my $movement_time = evaluate_space_movement($now, ($likelyhood_space{$task->{'id'}} or []));

    $task->{'current_utility'} = $utility * $likelyhood_time / ($time_estimate + $movement_time) / 1000000;
  }
  
  return $task;
}

sub evaluate_time_distribution {
  my ($time, $default, $spec) = @_;

  return $default unless @$spec;

  my $value = 0;
  foreach my $spec (sort @$spec) {
    if($spec =~ /^.constant:(.*)/) {
      $value += $1;
    } elsif($spec =~ /^.mulrange:(.*);(.*);(.*)/) {
      if($1 le $time and $time le $2) {
        $value = $value * $3 / 1000;
      }
    } elsif($spec =~ /^.mulhours:(\d+):(\d+)\+(\d+);(.+)/) {
      my $start = $1 * 60 + $2;
      my $end = $start + $3;
      my $multiplier = $4;
      my (undef, undef, undef, $hour, $minute) = localtime(str2time($time));
      my $minuteOfDay = $hour * 60 + $minute;
      if($start <= $minuteOfDay and $minuteOfDay < $end) {
        $value = $value * $multiplier / 1000;
      }
    } else {
      die "unknown distribution spec: $spec";
    }
  }

  return $value;
}

my $space_active_by_dimension;
my $space_topology;

sub evaluate_space_movement {
  my ($time, $spec) = @_;

  unless($space_topology) {
    fetch_space_topology();
  }

  unless($space_active_by_dimension) {
    fetch_space_active($time);
  }

  my $max = 0;

  foreach my $location (@$spec) {
    unless(space_by_name()->{$location}) {
      die "No space definition for location '$location'";
      next;
    }

    my $to = space_by_name()->{$location}->{'id'};
    my $currentLocation = $space_active_by_dimension->{space_by_name()->{$location}->{'dimension'}};
    my $from = defined $currentLocation? $currentLocation->{'id'}: -1;

    my $time = 0;

    if(exists $space_topology->{$to}->{$from}) {
      $time = $space_topology->{$to}->{$from};
    } elsif(exists $space_topology->{$to}->{-1}) {
      $time = $space_topology->{$to}->{-1};
    }

    if($time > $max) {
      $max = $time;
    }
  }

  return $max;
}

sub fetch_space_topology {
  $space_topology = {};

  foreach my $row (@{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} })}) {
  SELECT * FROM space_topology
EOSQL
    $space_topology->{$row->{'whereto'}}->{$row->{'wherefrom'}} = $row->{'seconds_estimate'};
  }
}

sub fetch_space_active {
  my ($time) = @_;

  my $cutoff = iso_full_date(str2time($time) - 600);

  $space_active_by_dimension = Secrets::activeSpaceBySchedule();

  foreach my $row (@{dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $cutoff)}) {
  SELECT * FROM space_active WHERE reached_at > ? ORDER BY reached_at ASC
EOSQL
    $space_active_by_dimension->{$row->{'dimension'}} = $row;
  }

  return $space_active_by_dimension;
}

sub resolveExpectation {
  my ($identifier) = @_;

  my $gidResolve = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $identifier);
  SELECT * FROM expectation WHERE gid = ?
EOSQL
  return $gidResolve->[0] if(@$gidResolve);

  my $titleResolve = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} }, $identifier);
  SELECT * FROM expectation WHERE title = ?
EOSQL
  return $titleResolve->[0] if(@$titleResolve);

  die "could not find any such expectation, maybe try expectation query?";
}

sub expectations {
  my $expectations = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} });
  SELECT * FROM expectation
EOSQL
  my $expUtilities = dbh()->selectall_arrayref(<<EOSQL, { Slice => {} });
  SELECT * FROM expectation_utilities
EOSQL
  my %expectations;
  foreach my $e (@$expectations) {
    $expectations{$e->{'id'}} = $e;
  }

  foreach my $exp (@$expUtilities) {
    push @{$expectations{$exp->{'expectation'}}->{'utilities'}}, $exp;
  }

  foreach my $e (@$expectations) {
    $e->{'current_value'} = $e->{'value'} + (time() - str2time($e->{'last_calculated'})) / 3.6; # 1000 per hour
    $e->{'current_utility'} = evaluate_expectation_utility($e->{'current_value'}, $e->{'utilities'});
  }

  return $expectations;
}

sub evaluate_expectation_utility {
  my ($value, $utilities) = @_;

  my $utility = 0;
  my @utilities = sort { $a->{'distribution'} cmp $b->{'distribution'} } @$utilities;

  foreach my $u (@utilities) {
    if($u->{'distribution'} =~ /^.flat:(\d+);(\d+);(\d+)/) {
      if($value >= $1 and $value < $2) {
        $utility = $3;
      }
    } elsif($u->{'distribution'} =~ /^.linear:(\d+);(\d+);(\d+);(\d+)/) {
      if($value >= $1 and $value < $2) {
        $utility = $3 + ($value - $1) / ($2 - $1) * ($4 - $3);
      }
    }
  }

  return $utility;
}

1;
