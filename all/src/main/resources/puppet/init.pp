file {'/home/michaelhuettermann/work/tmp/puppet/puppet.txt':
    ensure  => present,
    content => "Hello World!",
    mode   => 700,
}

file { "/home/michaelhuettermann/work/tmp/puppet":
    ensure => "directory",
    owner  => "michaelhuettermann",
    group  => "michaelhuettermann",
    mode   => 750,
    before  => File['/home/michaelhuettermann/work/tmp/puppet/puppet.txt'],
}
