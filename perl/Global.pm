package Global;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(dbh create_gid iso_date iso_full_date PUBLICATION_LOCAL PUBLICATION_PRIVATE PUBLICATION_TRANSPARENCY PUBLICATION_OFFER);

use strict;
use warnings;

use DBI;
use Date::Format;

my $dbh;

sub dbLocation() {
  if(defined $ENV{'HOME'} and $ENV{'HOME'} =~ /drahflow/) {
    return "$ENV{'HOME'}/utilator.db";
  }
  if(-d '/opt/utilator') {
    return '/opt/utilator/utilator.db';
  }
}

sub dbh() {
  return $dbh if $dbh;

  return $dbh = DBI->connect("dbi:SQLite:dbname=" . dbLocation() . "", "", "", {
      RaiseError     => 1,
      sqlite_unicode => 1,
    });
}

sub create_gid() {
  my $template = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
  1 while($template =~ s/x/[split m!!, "0123456789ABCDEF"]->[int(rand 16)]/e);
  1 while($template =~ s/y/[split m!!, "89AB"]->[int(rand 4)]/e);
  return $template;
}

sub iso_date($) {
  return time2str('%Y-%m-%d', shift);
}

sub iso_full_date($) {
  return time2str('%Y-%m-%dT%H:%M:%S', shift);
}

use constant PUBLICATION_LOCAL => 0;
use constant PUBLICATION_PRIVATE => 1;
use constant PUBLICATION_TRANSPARENCY => 2;
use constant PUBLICATION_OFFER => 3;

1;
