package io.citegraph.data;

import java.io.IOException;
import java.util.List;

import org.dblp.mmdb.BookTitle;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.RecordDb;
import org.dblp.mmdb.RecordDbInterface;
import org.xml.sax.SAXException;

/**
 * It parses dblp dataset and dumps into graph database
 */
public class DblpParser {
    public static void main(String[] args) {

        // we need to raise entityExpansionLimit because the dblp.xml has millions of entities
        System.setProperty("entityExpansionLimit", "10000000");

        if (args.length != 2) {
            System.err.format("Usage: java %s <dblp-xml-file> <dblp-dtd-file>\n", DblpParser.class.getName());
            System.exit(0);
        }
        String dblpXmlFilename = args[0];
        String dblpDtdFilename = args[1];

        System.out.println("building the dblp main memory DB ...");
        RecordDbInterface dblp;
        try {
            dblp = new RecordDb(dblpXmlFilename, dblpDtdFilename, false);
        }
        catch (final IOException ex) {
            System.err.println("cannot read dblp XML: " + ex.getMessage());
            return;
        }
        catch (final SAXException ex) {
            System.err.println("cannot parse XML: " + ex.getMessage());
            return;
        }
        System.out.format("MMDB ready: %d publs, %d pers\n\n", dblp.numberOfPublications(), dblp.numberOfPersons());

        System.out.println("finding most prolific author in dblp ...");
        for (Person person : dblp.getPersons()) {
            String name = person.getPrimaryName().name();
            for (Publication pub : person.getPublications()) {
                List<PersonName> authors = pub.getNames();
                String title = pub.getFields("title").iterator().next().value();
            }
        }

        System.out.println("done.");
    }
}


