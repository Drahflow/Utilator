package Query;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(sortedTasks);

use strict;
use warnings;

use Global qw(/./);
use Date::Format;

my %utility;
my %likelyhood_time;

sub resolveTask($) {
  my ($task) = (@_);

  return undef unless defined $task;

  my $now = iso_full_date(time);

  if($task eq 'c') {
    $task = [sortedTasks($now)]->[-1] or die "no current task";
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
      defined $_->{'description'} and $_->{'description'} =~ m/$mask/
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

  if(@{$utility{$task->{'id'}}} == 1 and not $likelyhood_time{$task->{'id'}}) {
    if($utility{$task->{'id'}}->[0] =~ /.constant:(.*)/) {
      $task->{'compiled_utility'} = $1 * 990 / 1000000;
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

    $task->{'current_utility'} = $utility * $likelyhood_time / $time_estimate / 1000000;
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
    } else {
      die "unknown distribution spec: $spec";
    }
  }

  return $value;
}

1;
