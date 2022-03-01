package com.kazurayam.materialstore.differ


import com.kazurayam.materialstore.resolvent.MProduct
import com.kazurayam.materialstore.resolvent.MProductGroup
import com.kazurayam.materialstore.resolvent.Reducer
import com.kazurayam.materialstore.filesystem.FileType

interface DifferDriver extends Reducer {

    MProductGroup differentiate(MProductGroup mProductGroup)

    MProduct differentiate(MProduct mProduct)

    boolean hasDiffer(FileType fileType)
}