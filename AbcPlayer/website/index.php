<?php
	define('_ABCPLAYER_', true);
	require('_header.php');
	
	$cur_ver = "1.1.0";
	
	$msi_path = "bin/AbcPlayer_$cur_ver.msi";
	$msi_size = round(@filesize($msi_path) / (1024 * 1024), 1) . "MB";
	
	$zip_path = "bin/AbcPlayer_$cur_ver.zip";
	$zip_size = round(@filesize($zip_path) / (1024 * 1024), 1) . "MB";
?>

<h2>Description</h2>
<p>
Use ABC Player to listen to .abc files as they would sound in 
<a href="http://www.lotro.com/">The Lord of the Rings Online</a>'s 
<a href="http://lorebook.lotro.com/wiki/Mechanics:Player_Music_System">player music system</a>.  
ABC Player is especially useful for previewing songs with multiple parts, to see how the parts 
sound with each other without having to get your band together in game.
</p>

<p style="color: yellow;">Version <?=$cur_ver?> is here... Now in stereo! Take a look at the 
<a href="changelog.php">Release Notes</a> to see what's new.</p>

<h2>Downloads</h2>
<p>
<a class="dnld" style="font-weight:bold;" href="<?=$msi_path?>">ABC Player v<?=$cur_ver?> 
	Installer</a> <?=$msi_size?><br />
<i>The installer will configure your computer to open .abc files using ABC Player.</i>
</p>
<p>
ABC Player requires <a class="dnld" href="http://java.com/">Java 6.0</a> or greater.  
You must have the 32-bit version of Java installed, even if you're running 64-bit Windows.
</p>
<p>
<a class="dnld" href="<?=$zip_path?>">AbcPlayer_<?=$cur_ver?>.zip</a> <?=$zip_size?> (advanced)<br />
<i>All of the files needed to run ABC Player in a .zip, if you prefer not to use the installer, or
want to try running ABC Player on another operating system (like 64-bit Java, MacOS, or Linux). 
Running ABC Player on another OS is completely untested and unsupported.</i>
</p>

<h2>How to use</h2>
<p>
ABC Player is fairly straightforward.  If you used the installer, you can double-click 
on an .abc file to play it.  You can also drag and drop an .abc file onto the player, or use File > Open.
</p>
<p>
If you have a multi-part song separated into multiple files, simply drag and drop all of the 
files in the song onto the player to open them as a single song.
</p>
<p>
You can export the song to a .wav (uncompressed audio) file using File > Save as Wave file.  You can then
use a program like <a href="http://lame.sourceforge.net/">LAME</a> to convert the .wav file to .mp3.
</p>

<h2>Preview</h2>
<p>
<img src="abcplayer_1.1.0.png" alt="Screenshot of ABC Player"/>
</p>
<p>
This is what the song pictured above sounds like when played in ABC Player:
</p>
<?php mp3('getaround', 'I Get Around'); ?>
<p>
If you want to try the song yourself, download <a class="dnld" href="files/getaround.abc">getaround.abc</a> 
and either open it in ABC Player, or get 5 friends together and play it in LotRO.
</p>

<?php
	require('_footer.php');
?>