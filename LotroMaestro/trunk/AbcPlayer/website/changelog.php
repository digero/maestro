<?php
	define('_ABCPLAYER_', true);
	$pagename = "Release Notes";
	require('_header.php');
?>
<h2>Version 1.3.0</h2>
<i>July 20, 2013</i>
<h3>New Features</h3>
<ul>
<li>Added support for the Pibgorn instrument. There are several notes that play 
    at the wrong pitch to match the broken behavior of the Pibgorn in the game.</li>
<li>Added a Solo button [S] to let you easily listen to a single part solo without 
    having to mute the rest of the tracks.</li>
<li>The instrument dropdown is now sorted by name.</li>
</ul>
<h3>Bug Fixes</h3>
<ul>
<li>The +pppp+ and +ffff+ dynamics specifiers should no longer cause errors.</li>
</ul>
<hr/>
<h2>Version 1.2.2</h2>
<i>January 6, 2012</i>
<h3>Bug Fixes</h3>
<ul>
<li>ABC Player now works with Java 6 Update 30+.</li>
<li>ABC Player's play speed should match LOTRO's play speed if a song has both 
    the M: and L: header fields specified.</li>
<li>The song position bar now lets you drag past 35:47 on long songs.</li>
<li>Misc tweaks to the UI.</li>
</ul>
<hr/>
<h2>Version 1.2.0</h2>
<i>March 15, 2011</i>
<h3>New Features</h3>
<ul>
<li>You can now export songs directly to MP3! Requires the (free) LAME mp3 
    converter.</li>
<li>The volume slider is back. If you're using Windows Vista or Windows 7, 
    it will use the system's per-application volume (kept in sync with the 
    volume set for ABC Player in the system volume mixer).</li>
<li>Changed the stereo effect to be less dependent on the order of the parts
    in the file, and more on the instrument used for the part. For example, 
    flute will always be panned to the left. If the same instrument is used in 
    multiple parts, the second part will be be panned to opposite speaker, 
    and the third part will be panned to the center.</li>
<li>If a part name includes the word "Left", it will always be panned to the
    left speaker. Likewise for the words "Right, and "Center" or "Middle".</li>
</ul>
<h3>Bug Fixes</h3>
<ul>
<li>When a long note is held on a woodwind instrument while stringed 
    instruments are playing many fast notes, the note on the woodwind should 
    no longer be cut off before it actually ends.</li>
<li>If the same note is repeated quickly on a woodwind instrument, the 
    difference between notes is sharper, and sounds more like it does in LOTRO.</li>
<li>Staccato (short) notes on woodwind instruments are more... staccato, to 
    sound more like they do in LOTRO.</li>
<li>Bagpipe drone notes (C, through B,) now sustain forever rather than fading 
    out after ~8 seconds, to match how they sound in LOTRO.</li>
</ul>
<hr/>
<h2>Version 1.1.0</h2>
<i>August 18, 2010</i>
<h3>New Features</h3>
<ul>
<li>Now in Stereo! In songs with multiple parts, the second and subsequent parts are panned 
	slightly to the right or left to emulate how it would sound playing with other people in the 
	game. (The stereo effect in ABC Player is slightly less than in the game).</li>
<li>You can now paste songs from the clipboard to open them. Use Ctrl+V to create a new song using 
	the text from the clipboard. Use Ctrl+Shift+V to add the text from the clipboard to the 
	current song (useful if pasting a multi-part song from thefatlute.com, for example).<br/>
    You can also use Ctrl+V and Ctrl+Shift+V with actual .abc files, as an alternative to 
	dragging and dropping the files.</li>
<li>You can now append additional parts to a song once it's already open, including the same song 
	multiple times. You can accomplish this in a variety of ways:
	<ul>
    <li>Choose "Append ABC file(s)..." from the File menu (Ctrl+Shift+O).</li>
    <li>Choose "Append from clipboard" from the File menu (Ctrl+Shift+V) if the file is on the 
		clipboard.</li>
    <li>Hold down Ctrl and drag/drop the file onto ABC Player.</li>
	</ul></li>
<li>Added a tempo slider to adjust the tempo of playback. Right-click on the slider to quickly 
	toggle between 0% and 100%.  Also removed the volume slider, as it didn't seem very useful.</li>
<li>ABC Player will now warn you about ABC errors that are specific to Lord of the Rings Online. 
	These include notes that are too low, too high, too short, too long; chords with too many notes; 
	or volume +dynamics+ inside chords. You can choose to disable these warnings if you want.</li>
<li>Added support for the following key signature modes in addition to major and minor:
	Dorian, Phrygian, Lydian, Mixolydian, Aeolian, Ionian, and Locrian.</li>
</ul>
<h3>Bug Fixes</h3>
<ul>
<li>ABC Player now handles notes with incorrect octave markers (like c,, or A') the same way that 
	LotRO does, rather than giving an error.  The comma always lowers the octave and the 
	apostrophe always raises the octave, regardless of whether the note letter is upper or lower 
	case.</li>
<li>Tuplets that contain chords should now have the correct note length.</li>
<li>Fixed an issue when determining the speed to play songs that have a meter with a denominator 
	that's not 4 (e.g. 6/8). Hopefully all songs should play at the correct speed now.</li>
<li>Reduced the volmue difference between +ppp+ and +fff+. ABC Player's volume dynamics should now 
	more closely match LotRO's.</li>
<li>Fixed the harp's ^a and b notes so they don't sound like clicks.</li>
<li>Occasionally when changing instruments, you'd hear the instruments playing at full volume for 
	a few seconds. This should no longer happen.</li>
<li>Removed the power of two restriction on the meter's denominator.</li>
<li>You can now change the instrument and see bar numbers even if the abc file is missing the 
	(required) "X:" line at the beginning. Note that the "X:" line is still required at the 
	beginning of each part in multi-part songs, even if the parts are in separate files.</li>
<li>ABC Player now ignores slurs () without generating errors, to mimic LotRO's behavior.</li>
<li>Playing a part that has note ties on the cowbell should no longer generate erroneous note tie 
	errors.</li>
</ul>
<hr/>
<h2>Version 1.0.0</h2>
<i>July 14, 2010</i>
<ul>
	<li>Initial release</li>
	<li>This hasn't been tested by anyone other than me, so it may fail on 
		certain ABC files. Let me know if you find an ABC file that plays in 
		LotRO but not in ABC Player.</li>
</ul>

<?php
	require('_footer.php');
?>