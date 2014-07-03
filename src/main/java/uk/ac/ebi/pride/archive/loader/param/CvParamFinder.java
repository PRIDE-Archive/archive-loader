package uk.ac.ebi.pride.archive.loader.param;

import uk.ac.ebi.pride.prider.repo.param.CvParam;

import java.util.Collection;

/**
 * Interface for finding all the cv params within a given object
 *
 * @author Rui Wang
 * @version $Id$
 */
public interface CvParamFinder<T> {

    Collection<CvParam> find(T object);
}
