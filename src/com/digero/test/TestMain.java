package com.digero.test;


public class TestMain
{
	public static void runAll()
	{
		LotroInstrumentTest.run();
	}

	public static void main(String[] args)
	{
		boolean assertsEnabled = false;
		assert assertsEnabled = true;
		if (!assertsEnabled)
		{
			System.err.println("Asserts must be enabled");
			System.exit(1);
		}

		try
		{
			runAll();
		}
		catch (AssertionError failure)
		{
			failure.printStackTrace();
			System.exit(1);
		}

		System.exit(0);
	}
}
