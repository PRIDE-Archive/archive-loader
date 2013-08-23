package uk.ac.ebi.pride.prider.loader.assay;

import uk.ac.ebi.pride.data.core.CvParam;
import uk.ac.ebi.pride.data.core.ParamGroup;
import uk.ac.ebi.pride.data.core.Person;
import uk.ac.ebi.pride.data.core.Software;
import uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class AssayFileSummary {
    private String name;
    private String shortLabel;
    private int numberOfProteins;
    private int numberOfPeptides;
    private int numberOfSpectra;
    private int numberOfUniquePeptides;
    private int numberOfIdentifiedSpectra;
    private boolean ms2Annotation;
    private boolean chromatogram;
    private final Set<CvParam> ptms;
    private final Set<Instrument> instruments;
    private final Set<Software> softwares;
    private final Set<Person> contacts;
    private ParamGroup additional;

    public AssayFileSummary() {
        this.name = null;
        this.shortLabel = null;
        this.numberOfProteins = 0;
        this.numberOfPeptides = 0;
        this.numberOfSpectra = 0;
        this.numberOfUniquePeptides = 0;
        this.numberOfIdentifiedSpectra = 0;
        this.ms2Annotation = false;
        this.chromatogram = false;
        this.ptms = new LinkedHashSet<CvParam>();
        this.instruments = new LinkedHashSet<Instrument>();
        this.softwares = new LinkedHashSet<Software>();
        this.contacts = new LinkedHashSet<Person>();
        this.additional = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    public int getNumberOfProteins() {
        return numberOfProteins;
    }

    public void setNumberOfProteins(int numberOfProteins) {
        this.numberOfProteins = numberOfProteins;
    }

    public int getNumberOfPeptides() {
        return numberOfPeptides;
    }

    public void setNumberOfPeptides(int numberOfPeptides) {
        this.numberOfPeptides = numberOfPeptides;
    }

    public int getNumberOfSpectra() {
        return numberOfSpectra;
    }

    public void setNumberOfSpectra(int numberOfSpectra) {
        this.numberOfSpectra = numberOfSpectra;
    }

    public int getNumberOfUniquePeptides() {
        return numberOfUniquePeptides;
    }

    public void setNumberOfUniquePeptides(int numberOfUniquePeptides) {
        this.numberOfUniquePeptides = numberOfUniquePeptides;
    }

    public int getNumberOfIdentifiedSpectra() {
        return numberOfIdentifiedSpectra;
    }

    public void setNumberOfIdentifiedSpectra(int numberOfIdentifiedSpectra) {
        this.numberOfIdentifiedSpectra = numberOfIdentifiedSpectra;
    }

    public boolean hasMs2Annotation() {
        return ms2Annotation;
    }

    public void setMs2Annotation(boolean ms2Annotation) {
        this.ms2Annotation = ms2Annotation;
    }

    public boolean hasChromatogram() {
        return chromatogram;
    }

    public void setChromatogram(boolean chromatogram) {
        this.chromatogram = chromatogram;
    }

    public Set<CvParam> getPtms() {
        return ptms;
    }

    public void addPtms(Collection<CvParam> ptms) {
        this.ptms.addAll(ptms);
    }

    public Set<Instrument> getInstruments() {
        return instruments;
    }

    public void addInstruments(Collection<Instrument> instruments) {
        this.instruments.addAll(instruments);
    }

    public Set<Software> getSoftwares() {
        return softwares;
    }

    public void addSoftwares(Collection<Software> softwares) {
        this.softwares.addAll(softwares);
    }

    public Set<Person> getContacts() {
        return contacts;
    }

    public void addContacts(Collection<Person> contacts) {
        this.contacts.addAll(contacts);
    }

    public ParamGroup getAdditional() {
        return additional;
    }

    public void setAdditional(ParamGroup additional) {
        this.additional = additional;
    }
}
