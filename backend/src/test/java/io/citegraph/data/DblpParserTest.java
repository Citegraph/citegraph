package io.citegraph.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DblpParserTest {

    @Test
    public void testSameCountry() {
        Assertions.assertFalse(DblpParser.mightSameCountry(
            "Signal Processing Group, Intelligent Automation, Inc., Rockville, MD 20855, USA",
            "Dalian Univ Technol, Res Ctr Informat & Control, Dalian, Liaoning, Peoples R China"));
        Assertions.assertTrue(DblpParser.mightSameCountry(
            "Department of Automation, Shanghai Jiaotong University, 800 Dong Chuan Road, Shanghai 200240, PR China",
            "Dalian Univ Technol, Res Ctr Informat & Control, Dalian, Liaoning, Peoples R China"));
        // aberdeen is not in our list, so we cannot reject hypothesis
        Assertions.assertTrue(DblpParser.mightSameCountry("Department of Computing Science, University of Aberdeen, Aberdeen",
            "Department of Artificial Intelligence, University of Edinburgh, Edinburgh, Great Britain"));
        Assertions.assertTrue(DblpParser.mightSameCountry("Dept. of Comput. Sci., Michigan State Univ., East Lansing, MI, USA",
            "Department of Computer Science, Michigan State University, East Lansing, MI 48824, U.S.A."));
        Assertions.assertFalse(DblpParser.mightSameCountry("China",
            "Department of Computer Science, Michigan State University, East Lansing, MI 48824, U.S.A."));
        Assertions.assertTrue(DblpParser.mightSameCountry("UK, united States",
            "Department of Computer Science, Michigan State University, East Lansing, MI 48824, U.S.A."));
    }

    @Test
    public void testEquivalentRegions() {
        Assertions.assertTrue(DblpParser.equivalentRegions("us", "usa"));
        Assertions.assertTrue(DblpParser.equivalentRegions("usa", "america"));
        Assertions.assertTrue(DblpParser.equivalentRegions("china", "hong kong"));
        Assertions.assertFalse(DblpParser.equivalentRegions("china", "us"));
        Assertions.assertFalse(DblpParser.equivalentRegions("uk", "us"));
        Assertions.assertFalse(DblpParser.equivalentRegions("mali", "us"));
        Assertions.assertFalse(DblpParser.equivalentRegions("mali", "brazil"));
        Assertions.assertTrue(DblpParser.equivalentRegions("brazil", "brazil"));
    }
}
