<?php
	header('Content-Type: text/html;charset=utf-8');

	$host_and_uri = $_SERVER['HTTP_HOST'] . rtrim(dirname($_SERVER['PHP_SELF']), '/\\');

	if (!defined('_ABCPLAYER_'))
	{
		header("Location: http://$host_and_uri/index.php");
		exit();
	}
	
	function sidebar($href, $text)
	{
		echo('<a ');
		if (basename($_SERVER['PHP_SELF']) == $href)
			echo('class="active" ');
		echo('href="' . $href . '">' . $text . "</a>\n");
	}
	
	function mp3($filename, $title)
	{
		global $host_and_uri;
		echo("<div><div id='$filename'><a href='http://$host_and_uri/mp3/$filename.mp3'>$filename.mp3</a></div></div>\n");
		echo("<script type='text/javascript'>");
		echo("AudioPlayer.embed('$filename', {soundFile: 'http://$host_and_uri/mp3/$filename.mp3'});");
		echo("</script>\n");
	}
	
	$title = "ABC Player";
	if (isset($pagename))
		$title .= " - " . $pagename;
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title><?=$title?></title>

<link rel="stylesheet" href="abcplayer.css" type="text/css" />
<link rel="icon" type="image/vnd.microsoft.icon" href="abcplayer.ico" />

<script type="text/javascript" src="mp3/audio-player.js"></script>
<script type="text/javascript">
	AudioPlayer.setup("http://<?= $host_and_uri ?>/mp3/player.swf", {
		width: 438, 
		transparentpagebg: "yes", 
	});
</script>
</head>

<body>
<div id="page">
	<div id="header">
		<img id="headericon" src="images/abcplayer_96.png" alt="" />
		<h1><a href="index.php">ABC Player</a></h1>
		<div class="description">for <i>The Lord of the Rings Online</i></div>
	</div>
	
	<div id="mainarea">
	<div id="sidebar">
		<div id="sidebarnav">
		<?php sidebar('index.php', 'Home'); ?>
		<?php sidebar('changelog.php', 'Release Notes'); ?>
		<?php sidebar('contact.php', 'Contact Me'); ?>
		<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KPX5YZW5SALAG">
			<img src="images/btn_donate_SM.gif" alt="Donate" title="PayPal - The safer, easier way to pay online!" width="74" height="21" border="0" />
		</a>
		</div>
		<?php /*
		<form action="https://www.paypal.com/cgi-bin/webscr" method="post">
		<div id="donate">
			<input type="hidden" name="cmd" value="_s-xclick"/>
			<input type="hidden" name="hosted_button_id" value="KPX5YZW5SALAG"/>
			<input type="image" src="images/btn_donate_SM.gif" name="submit" alt="Donate" title="PayPal - The safer, easier way to pay online!" width="74" height="21" />
			<img alt="" src="images/pixel.gif" width="1" height="1"/>
		</div>
		</form>
		*/ ?>
	</div>
	
	<div id="contentarea">
