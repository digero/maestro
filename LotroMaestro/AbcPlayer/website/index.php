<?php
	define('_ABCPLAYER_', true);
	require('_header.php');
?>

<h2>WELCOME</h2>
Preview .abc files as they would sound in <a href="http://www.lotro.com/">The Lord of the Rings Online</a>.

<h2>PREVIEW</h2>
Here's a screenshot of the main window, showing a song with
<div><img src="abcplayer.png" /></div>
<?php mp3('sunshine', 'Walking on Sunshine'); ?>

<?php
	require('_footer.php');
?>