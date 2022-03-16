package com.kazurayam.materialstore.filesystem

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kazurayam.materialstore.util.JsonUtil
import groovy.json.JsonSlurper

import java.nio.file.Path
import java.nio.file.Paths

final class IndexEntry implements Comparable, JSONifiable, TemplateReady {

    public static final IndexEntry NULL_OBJECT =
            new IndexEntry(
                    new MObject(ID.NULL_OBJECT, FileType.NULL_OBJECT),
                    Metadata.NULL_OBJECT)

    private MObject mObject_
    private Metadata metadata_

    IndexEntry(MObject mObject, Metadata metadata) {
        this.mObject_ = mObject
        this.metadata_ = metadata
    }

    static IndexEntry parseLine(String line) throws IllegalArgumentException {
        Objects.requireNonNull(line)
        List<String> items = line.split('\\t') as List<String>
        ID id = null
        FileType fileType = null
        Metadata metadata = null
        if (items.size() > 0) {
            String item1 = items[0]
            if (! ID.isValid(item1)) {
                throw new IllegalArgumentException("invalid ID")
            }
            id = new ID(item1)
            if (items.size() > 1) {
                fileType = FileType.getByExtension(items[1])
                if (fileType == FileType.UNSUPPORTED) {
                    throw new IllegalArgumentException("unsupported file extension")
                }
                if (items.size() > 2) {
                    try {
                        Object obj = new JsonSlurper().parseText(items[2])
                        assert obj instanceof Map
                        metadata = Metadata.builder((Map)obj).build()
                    } catch (Exception e) {
                        throw new IllegalArgumentException("unable to parse metadata part")
                    }
                }
            }
        }
        if (id != null && fileType != null && metadata != null) {
            return new IndexEntry(new MObject(id, fileType), metadata)
        }
        return null   // blank line returns null
    }

    private MObject getMObject() {
        return mObject_
    }

    Path getFileName() {
        MObject mObject = getMObject()
        return Paths.get(mObject.getID().toString() + "." + mObject.getFileType().getExtension())
    }

    FileType getFileType() {
        return getMObject().getFileType()
    }

    ID getID() {
        return getMObject().getID()
    }

    String getShortId() {
        return getID().getShortSha1()
    }

    Metadata getMetadata() {
        return metadata_
    }

    @Override
    boolean equals(Object obj) {
        if (! obj instanceof IndexEntry) {
            return false
        }
        IndexEntry other = (IndexEntry)obj
        return this.getFileType().equals(other.getFileType()) &&
                this.getMetadata().equals(other.getMetadata())
    }

    @Override
    int hashCode() {
        int hash = 7
        hash = 31 * hash + this.getMObject().hashCode()
        hash = 31 * hash + this.getMetadata().hashCode()
        return hash
    }

    @Override
    String toString() {
        return toJson()
    }

    @Override
    String toJson() {
        StringBuilder sb = new StringBuilder()
        sb.append("{")
        sb.append("\"id\": " + this.getMObject().getID().toJson())
        sb.append(",")
        sb.append("\"fileType\": " + this.getMObject().getFileType().toJson())
        sb.append(",")
        sb.append("\"metadata\": " + this.getMetadata().toString())
        sb.append("}")
        return JsonUtil.prettyPrint(sb.toString())
    }

    @Override
    Map<String, Object> toTemplateModel() {
        // convert JSON string to Java Map
        Map<String, Object> map = new Gson().fromJson(toJson(), Map.class)
        return map
    }

    @Override
    String toTemplateModelAsJSON() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create()
        Map<String, Object> model = toTemplateModel()
        return gson.toJson(model)
    }

    @Override
    int compareTo(Object obj) {
        if (! obj instanceof IndexEntry) {
            throw new IllegalArgumentException("obj is not an instance of IndexEntry")
        }
        IndexEntry other = (IndexEntry)obj
        int comparisonByMetadata = this.getMetadata() <=> other.getMetadata()
        if (comparisonByMetadata == 0) {
            int comparisonByFileType = this.getFileType() <=> other.getFileType()
            if (comparisonByFileType == 0) {
                return this.getID() <=> other.getID()
            } else {
                return comparisonByFileType
            }
        } else {
            return comparisonByMetadata
        }
    }
}
