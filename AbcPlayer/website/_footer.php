<?php
	if (!defined('_ABCPLAYER_'))
	{
		$host = $_SERVER['HTTP_HOST'];
		$uri = rtrim(dirname($_SERVER['PHP_SELF']), '/\\');
		header("Location: http://$host$uri/index.php");
		exit();
	}
?>

	</div> <!-- contentarea -->
	</div> <!-- mainarea -->
	
	<div id="footer">
		Created by Digero of Landroval<br />
		Copyright &copy; 2010-2013 Ben Howell<br />
		No affiliation with Turbine, Inc. or Warner Bros.
		<div id="copyright">
			<a href="http://www.free-css-templates.com/">Free CSS Templates</a> | <a href="http://www.openwebdesign.org/">pro web design</a>
		</div>
	</div>


</div>
</body>

</html>