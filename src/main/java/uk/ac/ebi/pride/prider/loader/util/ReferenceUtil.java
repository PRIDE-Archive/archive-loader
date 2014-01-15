package uk.ac.ebi.pride.prider.loader.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.ebi.pride.prider.loader.exception.SubmissionLoaderException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: rcote
 * Date: 13/02/13
 * Time: 11:19
 */
public class ReferenceUtil {

    public static PubMedReference getPubmedReference(String pubmedId) throws SubmissionLoaderException {

        try {
            //try and connect to ncbi
            String ncbiUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&id=" + pubmedId;
            URL url = new URL(ncbiUrl);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(url.openStream());
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("DocSum");

            if (nList.getLength() == 1) {

                PubMedReference ref = new PubMedReference();

                NodeList items = doc.getElementsByTagName("Item");
                for (int i = 0; i < items.getLength(); i++) {

                    Node node = items.item(i);
                    String nodeName = node.getAttributes().getNamedItem("Name").getFirstChild().getNodeValue();
                    if (nodeName.equals("PubDate")) {
                        ref.setDate(getValue(node));
                    }
                    if (nodeName.equals("Source")) {
                        ref.setSource(getValue(node));
                    }
                    if (nodeName.equals("Author")) {
                        ref.addAuthor(getValue(node));
                    }
                    if (nodeName.equals("Title")) {
                        ref.setTitle(getValue(node));
                    }
                    if (nodeName.equals("Volume")) {
                        ref.setVolume(getValue(node));
                    }
                    if (nodeName.equals("Issue")) {
                        ref.setIssue(getValue(node));
                    }
                    if (nodeName.equals("Pages")) {
                        ref.setPages(getValue(node));
                    }
                    if (nodeName.equals("doi")) {
                        ref.setDoi(getValue(node));
                    }
                    if (nodeName.equals("pubmed")) {
                        ref.setPmid(getValue(node));
                    }
                }

                return ref;

            } else {
                //nothing found
                return null;
            }

        } catch (Exception e) {
            throw new IllegalStateException("Error retrieving pubmed citation for " + pubmedId, e);
        }

    }


    private static String getValue(Node node) {
        //<Item Name="PubDate" Type="Date">2006 Feb 28</Item>
        if (node.getFirstChild() != null) {
            return node.getFirstChild().getNodeValue();
        } else {
            return node.getNodeValue();
        }

    }

    public static class PubMedReference {

        private String date;
        private String title;
        private String pmid;
        private List<String> authors = new ArrayList<String>();
        private String source;
        private String volume;
        private String issue;
        private String pages;
        private String doi;

        public void setDate(String date) {
            this.date = date;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setPmid(String pmid) {
            this.pmid = pmid;
        }

        public void addAuthor(String author) {
            authors.add(author);
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public void setIssue(String issue) {
            this.issue = issue;
        }

        public void setPages(String pages) {
            this.pages = pages;
        }

        public void setDoi(String doi) {
            this.doi = doi;
        }

        public String toCitation() {
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> i = authors.iterator(); i.hasNext(); ) {
                sb.append(i.next());
                if (i.hasNext()) {
                    sb.append(", ");
                } else {
                    sb.append("; ");
                }
            }
            sb.append(title).append(", ").append(source).append(", ").append(date).append(", ");
            if (volume != null) {
                sb.append(volume).append(", ");
            }
            if (issue != null) {
                sb.append(issue).append(", ");
            }
            if (pages != null) {
                sb.append(pages).append(", ");
            }
            return sb.toString();
        }

        public String getDOI() {
            return doi;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("PubMedReference");
            sb.append("{date='").append(date).append('\'');
            sb.append(", title='").append(title).append('\'');
            sb.append(", pmid='").append(pmid).append('\'');
            sb.append(", authors=").append(authors);
            sb.append(", source='").append(source).append('\'');
            sb.append(", volume='").append(volume).append('\'');
            sb.append(", issue='").append(issue).append('\'');
            sb.append(", pages='").append(pages).append('\'');
            sb.append(", doi='").append(doi).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        PubMedReference pubmedReference = ReferenceUtil.getPubmedReference(args[0]);
        System.out.println(pubmedReference.toCitation());
    }
}
