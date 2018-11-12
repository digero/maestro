package com.digero.test;

import com.digero.common.abc.LotroInstrument;

public class LotroInstrumentTest
{
	public static boolean run()
	{
		String allInstruments = "";
		for (LotroInstrument instrument : LotroInstrument.values())
			allInstruments += instrument + " ";

		for (LotroInstrument instrument : LotroInstrument.values())
		{
			test(instrument, instrument.name());
			test(instrument, instrument.toString());
			test(instrument, allInstruments + instrument); // Last match wins
		}

		test(LotroInstrument.LUTE_OF_AGES, "Lute");
		test(LotroInstrument.LUTE_OF_AGES, "the lute part");
		test(LotroInstrument.LUTE_OF_AGES, "Song - Guitar");
		test(LotroInstrument.LUTE_OF_AGES, " Lute of  Ages");
		test(LotroInstrument.LUTE_OF_AGES, "X AgeLute X");
		test(LotroInstrument.LUTE_OF_AGES, "LuteA");
		test(LotroInstrument.LUTE_OF_AGES, "loa 123");
		test(LotroInstrument.LUTE_OF_AGES, "Basic Lute Song - Lute of Ages");
		test(LotroInstrument.LUTE_OF_AGES, "Fiddle Song - Lute of Ages");
		test(LotroInstrument.LUTE_OF_AGES, "Bassoon Song - Lute");
		test(LotroInstrument.LUTE_OF_AGES, "Harp Song - Lute");
		test(LotroInstrument.LUTE_OF_AGES, "Clarinet Song - Lute");
		test(LotroInstrument.LUTE_OF_AGES, "Basic Horn Song - Lute");

		test(LotroInstrument.BASIC_LUTE, "Lute Song - basiclute 2");
		test(LotroInstrument.BASIC_LUTE, "Song - New Lute 3");
		test(LotroInstrument.BASIC_LUTE, "LuteB");
		test(LotroInstrument.BASIC_LUTE, "Song - banjo 1");
		test(LotroInstrument.BASIC_LUTE, "Basic Bassoon Song - Basic Lute");

		test(LotroInstrument.BASIC_HARP, "Song - Harp 2");
		test(LotroInstrument.BASIC_HARP, "Song - basic harp 3");
		test(LotroInstrument.BASIC_HARP, "Song - BasicHarp 4");

		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "Harp - Misty  Mountain   Harp");
		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "Song - MM Harp");
		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "Song - MistyMountainHarp");
		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "Song - Misty Harp");
		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "Song - mm harp 2");
		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "Song - mmh");
		test(LotroInstrument.MISTY_MOUNTAIN_HARP, "misty_harp");

		test(LotroInstrument.BARDIC_FIDDLE, "Fiddle");
		test(LotroInstrument.BARDIC_FIDDLE, "Violin");
		test(LotroInstrument.BARDIC_FIDDLE, "Song - Bardic Fiddle");
		test(LotroInstrument.BARDIC_FIDDLE, "Song - B Fiddle 2");

		test(LotroInstrument.BASIC_FIDDLE, "BasicFiddle");
		test(LotroInstrument.BASIC_FIDDLE, "basic_fiddle");
		test(LotroInstrument.BASIC_FIDDLE, "A basic fiddle");

		test(LotroInstrument.LONELY_MOUNTAIN_FIDDLE, "a lonelyfiddle");
		test(LotroInstrument.LONELY_MOUNTAIN_FIDDLE, "Song - LM Fiddle");
		test(LotroInstrument.LONELY_MOUNTAIN_FIDDLE, "lonely_fiddle");

		test(LotroInstrument.SPRIGHTLY_FIDDLE, "Song - Sprightly Fiddle");
		test(LotroInstrument.SPRIGHTLY_FIDDLE, "SprightlyFiddle");

		test(LotroInstrument.STUDENT_FIDDLE, "Student's Fiddle");
		test(LotroInstrument.STUDENT_FIDDLE, "Students Fiddle");
		test(LotroInstrument.STUDENT_FIDDLE, "StudentFiddle");

		test(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, "Traveler's Trusty Fiddle 2");
		test(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, "Travellers Trusty Fiddle 3");
		test(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, "travelerfiddle 4");
		test(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, "trusty fiddle 5");
		test(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, "TT fiddle 5");
		test(LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, "travellers_fiddle");

		test(LotroInstrument.BASIC_THEORBO, "Song - THEORBO");
		test(LotroInstrument.BASIC_THEORBO, "Song - basic  theorbo");
		test(LotroInstrument.BASIC_THEORBO, "Theo");
		test(LotroInstrument.BASIC_THEORBO, "The bass part");

		test(LotroInstrument.BASIC_FLUTE, "Song - BasicFlute 3");
		test(LotroInstrument.BASIC_FLUTE, "Song - a flute part");

		test(LotroInstrument.BASIC_CLARINET, "Song - Basic Clarinet");
		test(LotroInstrument.BASIC_CLARINET, "Song - the clarinet");

		test(LotroInstrument.BASIC_HORN, "Song - Basic horn");

		test(LotroInstrument.BASIC_BASSOON, "Bassoon");
		test(LotroInstrument.BASIC_BASSOON, "Song - Basic bassoon");
		test(LotroInstrument.BASIC_BASSOON, "Alonely bassoon");

		test(LotroInstrument.LONELY_MOUNTAIN_BASSOON, "Song - lonely mountain bassoon");
		test(LotroInstrument.LONELY_MOUNTAIN_BASSOON, "Lonely bassoon");
		test(LotroInstrument.LONELY_MOUNTAIN_BASSOON, "lonely_bassoon");

		test(LotroInstrument.BRUSQUE_BASSOON, "1 Brusque Bassoon");
		test(LotroInstrument.BRUSQUE_BASSOON, "2 Brusk  bassoon");

		test(LotroInstrument.BASIC_BAGPIPE, "Bagpipe 2");
		test(LotroInstrument.BASIC_BAGPIPE, "A BasicBagpipe");
		test(LotroInstrument.BASIC_BAGPIPE, "A Bag pipe");
		test(LotroInstrument.BASIC_BAGPIPE, "The bag pipe");

		test(LotroInstrument.BASIC_PIBGORN, "a basic pibgorn");
		test(LotroInstrument.BASIC_PIBGORN, "The Pibgorn 2");

		test(LotroInstrument.BASIC_DRUM, "Song - Drum");
		test(LotroInstrument.BASIC_DRUM, "Song - Drums 2");
		test(LotroInstrument.BASIC_DRUM, "Song - Basic Drums 2");

		test(LotroInstrument.MOOR_COWBELL, "Song - Moor Cowbell");
		test(LotroInstrument.MOOR_COWBELL, "Song - More Cowbell");

		test(LotroInstrument.BASIC_COWBELL, "Song - BasicCowbell 2");
		test(LotroInstrument.BASIC_COWBELL, "Song - Cowbell");

		test(null, "Dilute");
		test(null, "Sharp");
		test(null, "Basic Fiddlesticks");
		test(null, "Theorboat");
		test(null, "Fluted");
		test(null, "Eclarinet");
		test(null, "Shorn");
		test(null, "Lonely Mountain Bassoonet");
		test(null, "Bagpiped");
		test(null, "bag");
		test(null, "pipes");

		return true;
	}

	private static void test(LotroInstrument expected, String text)
	{
		LotroInstrument actual = LotroInstrument.findInstrumentName(text, null);
		assert expected == actual : "Instrument Name Test Failed!\nSearch string: " + text + "\nExpected: " + expected
				+ "\nActual: " + actual;
	}
}
