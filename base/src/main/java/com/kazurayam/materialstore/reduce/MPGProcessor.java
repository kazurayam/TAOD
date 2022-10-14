package com.kazurayam.materialstore.reduce;

import com.kazurayam.materialstore.filesystem.MaterialstoreException;

public interface MPGProcessor {

    MaterialProductGroup process(MaterialProductGroup mpg) throws MaterialstoreException;

}
