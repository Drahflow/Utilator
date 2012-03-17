package Global;

use Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(dbh create_gid PUBLICATION_LOCAL PUBLICATION_PRIVATE PUBLICATION_TRANSPARENCY PUBLICATION_OFFER);

use strict;
use warnings;

use DBI;

my $dbh;

sub dbh() {
  return $dbh if $dbh;

  return $dbh = DBI->connect("dbi:SQLite:dbname=$ENV{HOME}/utilator.db", "", "");
}

sub create_gid() {
  my $template = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx';
  1 while($template =~ s/x/[split m!!, "0123456789ABCDEF"]->[int(rand 16)]/e);
  1 while($template =~ s/y/[split m!!, "89AB"]->[int(rand 4)]/e);
  return $template;
}

use constant PUBLICATION_LOCAL => 0;
use constant PUBLICATION_PRIVATE => 1;
use constant PUBLICATION_TRANSPARENCY => 2;
use constant PUBLICATION_OFFER => 3;

1;
