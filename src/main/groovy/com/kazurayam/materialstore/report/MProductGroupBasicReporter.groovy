package com.kazurayam.materialstore.report

import com.kazurayam.materialstore.MaterialstoreException
import com.kazurayam.materialstore.filesystem.Store

import com.kazurayam.materialstore.reduce.differ.DifferUtil
import com.kazurayam.materialstore.filesystem.FileTypeDiffability
import com.kazurayam.materialstore.filesystem.JobName
import com.kazurayam.materialstore.filesystem.Material
import com.kazurayam.materialstore.filesystem.MaterialList
import com.kazurayam.materialstore.filesystem.metadata.IdentifyMetadataValues
import com.kazurayam.materialstore.filesystem.metadata.IgnoreMetadataKeys
import com.kazurayam.materialstore.filesystem.QueryOnMetadata
import com.kazurayam.materialstore.reduce.MProduct
import com.kazurayam.materialstore.reduce.MProductGroup
import com.kazurayam.materialstore.report.markupbuilder_templates.IgnoreMetadataKeysTemplate
import com.kazurayam.materialstore.report.markupbuilder_templates.MetadataTemplate
import com.kazurayam.materialstore.report.markupbuilder_templates.QueryOnMetadataTemplate
import groovy.xml.MarkupBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Path

final class MProductGroupBasicReporter extends MProductGroupReporter {

    private static final Logger logger = LoggerFactory.getLogger(MProductGroupBasicReporter.class)

    private Store store_

    private JobName jobName_

    private Double criteria_ = 0.0d

    MProductGroupBasicReporter(Store store, JobName jobName) {
        Objects.requireNonNull(store)
        Objects.requireNonNull(jobName)
        this.store_ = store
        this.jobName_ = jobName
    }

    @Override
    void setCriteria(Double criteria) {
        if (criteria < 0.0 || 100.0 < criteria) {
            throw new IllegalArgumentException("criteria(${criteria}) must be in the range of [0,100)")
        }
        this.criteria_ = criteria
    }

    @Override
    Path report(MProductGroup mProductGroup, String fileName) {
        Path reportFile = store_.getRoot().resolve(fileName)
        this.report(mProductGroup, reportFile)
        return reportFile
    }

    @Override
    void report(MProductGroup mProductGroup, Path filePath) {
        Objects.requireNonNull(mProductGroup)
        Objects.requireNonNull(filePath)
        //
        if (! mProductGroup.isReadyToReport()) {
            throw new MaterialstoreException(
                    "given MProductGroup is not ready to report. mProductGroup=" +
                            mProductGroup.toString())
        }
        //
        StringWriter sw = new StringWriter()
        MarkupBuilder mb = new MarkupBuilder(sw)
        mb.html(lang: "en") {
            head() {
                meta(charset: "utf-8")
                meta(name: "viewport", content: "width=device-width, initial-scale=1")
                mkp.comment("Bootstrap")
                link(href: "https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/css/bootstrap.min.css",
                        rel: "stylesheet",
                        integrity: "sha384-KyZXEAg3QhqLMpG8r+8fhAXLRk2vvoC2f3B09zVXn8CA5QIVfZOJ3BCsw2P0p/We",
                        crossorigin: "anonymous")
                style(StyleHelper.loadStyleFromClasspath())
                title(jobName_.toString())
            }
            body() {
                div(class: "container") {
                    h1(class: "title", getTitle(filePath)) {
                        button(class: "btn btn-secondary",
                                type: "button",
                                "data-bs-toggle":   "collapse",
                                "data-bs-target":   "#collapsingHeader",
                                "aria-expanded": "false",
                                "aria-controls": "collapsingHeader",
                                "About")
                    }
                    div(id: "collapsingHeader", class: "collapse header") {
                        dl() {
                            dt("Root path :")
                            dd(store_.getRoot().normalize().toString())
                            dt("JobName :")
                            dd(jobName_.toString())
                            //
                            dt("Left MaterialList specification")
                            MaterialList left = mProductGroup.getMaterialListLeft()
                            if (left != MaterialList.NULL_OBJECT) {
                                dd() {
                                    dl() {
                                        dt("JobTimestamp :")
                                        dd(left.getJobTimestamp().toString())
                                        dt("QueryOnMetadata :")
                                        dd() {
                                            new QueryOnMetadataTemplate(left.getQueryOnMetadata())
                                                    .toSpanSequence(mb)
                                        }
                                    }
                                }
                            } else {
                                dd("not set")
                            }
                            //
                            dt("Right MaterialList specification")
                            MaterialList right = mProductGroup.getMaterialListRight()
                            if (right != MaterialList.NULL_OBJECT) {
                                dd() {
                                    dl() {
                                        dt("JobTimestamp :")
                                        dd(right.getJobTimestamp().toString())
                                        dt("QueryOnMetadata :")
                                        dd() {
                                            new QueryOnMetadataTemplate(right.getQueryOnMetadata()).toSpanSequence(mb)
                                        }
                                    }
                                }
                            } else {
                                dd("not set")
                            }
                            //
                            dt("IgnoreMetadataKeys")
                            if (mProductGroup.getIgnoreMetadataKeys() != IgnoreMetadataKeys.NULL_OBJECT) {
                                dd() {
                                    new IgnoreMetadataKeysTemplate(mProductGroup.getIgnoreMetadataKeys()).toSpanSequence(mb)
                                }
                            } else {
                                dd("not set")
                            }
                        }
                    }
                    div(class: "accordion",
                            id: "diff-contents") {
                        mProductGroup.eachWithIndex { MProduct mProduct, int index ->
                            div(id: "accordion${index+1}",
                                    class: "accordion-item") {
                                h2(id: "heading${index+1}",
                                        class: "accordion-header") {
                                    button(class: "accordion-button",
                                            type: "button",
                                            "data-bs-toggle": "collapse",
                                            "data-bs-target": "#collapse${index+1}",
                                            "area-expanded": "false",
                                            "aria-controls": "collapse${index+1}") {

                                        Double diffRatio = mProduct.getDiffRatio()
                                        Boolean toBeWarned = decideToBeWarned(diffRatio, criteria_)
                                        String warningClass = getWarningClass(toBeWarned)
                                        span(class: "ratio ${warningClass}",
                                                "${DifferUtil.formatDiffRatioAsString(diffRatio)}")
                                        span(class: "fileType",
                                                mProduct.getFileTypeExtension())
                                        span(class: "description",
                                                mProduct.getDescription())
                                    }
                                }
                                div(id: "collapse${index+1}",
                                        class: "according-collapse collapse",
                                        "aria-labelledby": "heading${index+1}",
                                        "data-bs-parent": "#diff-contents") {
                                    mb.div(class: "accordion-body") {
                                        makeModalSubsection(mb, mProduct, index+1)
                                        //
                                        Context context = new Context(
                                                mProductGroup.getMaterialListLeft().getQueryOnMetadata(),
                                                mProductGroup.getMaterialListRight().getQueryOnMetadata(),
                                                mProductGroup.getIgnoreMetadataKeys(),
                                                mProductGroup.getIdentifyMetadataValues()
                                        )
                                        makeMaterialSubsection(mb, "left", mProduct.getLeft(), context)
                                        makeMaterialSubsection(mb, "right", mProduct.getRight(), context)
                                        makeMaterialSubsection(mb, "diff", mProduct.getDiff(), context)
                                    }
                                }
                            }
                        }
                    }
                }
                mkp.comment("Bootstrap")
                script(src: "https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/js/bootstrap.bundle.min.js",
                        integrity: "sha384-U1DAWAznBHeqEIlVSCgzq+c9gqGAJn5c/t99JyeKa9xxaYpSvHU5awsuZVVFIhvj",
                        crossorigin: "anonymous", "")
            }
        }
        filePath.toFile().text = "<!doctype html>\n" + sw.toString()
    }

    private static void makeModalSubsection(MarkupBuilder mb, MProduct mProduct, Integer count) {
        Material right = mProduct.getRight()
        mb.div(class: "show-diff") {
            if (right.getDiffability() == FileTypeDiffability.AS_IMAGE) {
                String imageModalId = "imageModal${count}"
                String imageModalTitleId = "imageModalLabel${count}"
                String carouselId = "carouselControl${count}"
                // Show 3 images in a Modal
                mkp.comment("Button trigger modal")
                button(type: "button", class: "btn btn-primary",
                        "data-bs-toggle": "modal",
                        "data-bs-target": "#${imageModalId}",
                        "Show diff in Modal")
                mkp.comment("Modal to show 3 images: Left/Diff/Right")
                div(class: "modal fade",
                        id:"${imageModalId}",
                        tabindex: "-1",
                        "aria-labelledby": "imageModalLabel", "aria-hidden": "true") {
                    div(class: "modal-dialog modal-fullscreen"){
                        div(class: "modal-content") {
                            div(class: "modal-header") {
                                h5(class: "modal-title",
                                        id: "${imageModalTitleId}") {
                                    span("${mProduct.getDiffRatioAsString()} ${mProduct.getFileTypeExtension()} ${mProduct.getQueryOnMetadata()}")
                                    button(type: "button",
                                            class: "btn-close",
                                            "data-bs-dismiss": "modal",
                                            "aria-label": "Close",
                                            "")
                                }
                            }
                            div(class: "modal-body") {
                                mkp.comment("body")
                                div(id: "${carouselId}",
                                        class: "carousel slide",
                                        "data-bs-ride": "carousel") {
                                    div(class: "carousel-inner") {
                                        div(class: "carousel-item") {
                                            h3(class: "centered","Left")
                                            img(class: "img-fluid d-block w-75 centered",
                                                    alt: "left",
                                                    src: mProduct.getLeft()
                                                            .getRelativeURL())
                                        }
                                        div(class: "carousel-item active") {
                                            h3(class: "centered","Diff")
                                            img(class: "img-fluid d-block w-75 centered",
                                                    alt: "diff",
                                                    src: mProduct.getDiff()
                                                            .getRelativeURL())
                                        }
                                        div(class: "carousel-item") {
                                            h3(class: "centered","Right")
                                            img(class: "img-fluid d-block w-75 centered",
                                                    alt: "right",
                                                    src: mProduct.getRight()
                                                            .getRelativeURL())
                                        }
                                    }
                                    button(class: "carousel-control-prev",
                                            type: "button",
                                            "data-bs-target": "#${carouselId}",
                                            "data-bs-slide": "prev") {
                                        span(class: "carousel-control-prev-icon",
                                                "aria-hidden": "true","")
                                        span(class: "visually-hidden",
                                                "Previous")
                                    }
                                    button(class: "carousel-control-next",
                                            type: "button",
                                            "data-bs-target": "#${carouselId}",
                                            "data-bs-slide": "next") {
                                        span(class: "carousel-control-next-icon",
                                                "aria-hidden": "true","")
                                        span(class: "visually-hidden",
                                                "Next")
                                    }
                                }
                            }
                            div(class: "modal-footer") {
                                button(type: "button", class: "btn btn-secondary",
                                        "data-bs-dismiss": "modal", "Close")
                            }
                        }
                    }
                }
            } else if (right.getDiffability() == FileTypeDiffability.AS_TEXT) {
                String textModalId = "textModal${count}"
                String textModalTitleId = "textModalLabel${count}"
                mkp.comment("Button trigger modal")
                button(type: "button", class: "btn btn-primary",
                        "data-bs-toggle": "modal",
                        "data-bs-target": "#${textModalId}",
                        "Show diff in Modal")
                mkp.comment("Modal to show texts diff")
                div(class: "modal fade",
                        id: "${textModalId}",
                        tabindex: "-1",
                        "aria-labelledby": "textModalLabel", "aria-hidden": "true") {
                    div(class: "modal-dialog modal-fullscreen") {
                        div(class: "modal-content") {
                            div(class: "modal-header") {
                                h5(class: "modal-title",
                                        id: "${textModalTitleId}") {
                                    span("${mProduct.getDiffRatioAsString()} ${mProduct.getFileTypeExtension()} ${mProduct.getQueryOnMetadata()}")
                                    button(type: "button",
                                            class: "btn-close",
                                            "data-bs-dismiss": "modal",
                                            "aria-label": "Close",
                                            "")
                                }
                            }
                            div(class: "modal-body") {
                                mkp.comment("body")
                                iframe(src: mProduct.getDiff().getRelativeURL(),
                                        title: "TextDiff", "")
                            }
                            div(class: "modal-footer") {
                                button(type: "button",
                                        class: "btn btn-secondary",
                                        "data-bs-dismiss": "modal",
                                        "Close")
                            }
                        }
                    }
                }
            } else {
                //logger.warn("right.getDiffability() returned ${right.getDiffability()}. What to do with this? ${right.toString()}")
            }
        }
    }

    /**
     *
     * @param mb
     * @param name
     * @param material
     * @param context　
     */
    private static void makeMaterialSubsection(MarkupBuilder mb, String name, Material material,
                                               Context context) {
        mb.div(class: "show-detail") {
            h2(name)
            dl(class: "detail") {
                dt("Material URL")
                dd() {
                    a(href: material.getRelativeURL(),
                            target: name,
                            material.getRelativeURL())
                }
                //
                dt("fileType")
                dd(material.getIndexEntry().getFileType().getExtension())
                //
                dt("metadata")
                //dd(material.getIndexEntry().getMetadata().toString())
                dd() {
                    new MetadataTemplate(material.getIndexEntry().getMetadata()).toSpanSequence(
                            mb,
                            (QueryOnMetadata)context.getLeftQueryOnMetadata(),
                            (QueryOnMetadata)context.getRightQueryOnMetadata(),
                            (IgnoreMetadataKeys)context.getIgnoreMetadataKeys(),
                            (IdentifyMetadataValues)context.getIdentifyMetadataValues()
                    )
                }
                if (material.getMetadata().toURL() != null) {
                    dt("Source URL")
                    dd() {
                        a(href: material.getMetadata().toURL().toExternalForm(),
                                target: "source",
                                material.getMetadata().toURL().toExternalForm())
                    }
                }
            }
        }
    }

    static Boolean decideToBeWarned(Double diffRatio, Double criteria) {
        return diffRatio > criteria
    }

    static String getWarningClass(boolean toBeWarned) {
        if (toBeWarned) {
            return "warning"
        } else {
            return ""
        }
    }

    /**
     *
     */
    class Context {
        private QueryOnMetadata left
        private QueryOnMetadata right
        private IgnoreMetadataKeys ignoreMetadataKeys
        private IdentifyMetadataValues identifyMetadataValues
        Context(QueryOnMetadata left, QueryOnMetadata right,
                IgnoreMetadataKeys ignoreMetadataKeys,
                IdentifyMetadataValues identifyMetadataValues) {
            this.left = left
            this.right = right
            this.ignoreMetadataKeys = ignoreMetadataKeys
            this.identifyMetadataValues = identifyMetadataValues
        }
        QueryOnMetadata getLeftQueryOnMetadata() {
            return this.left
        }
        QueryOnMetadata getRightQueryOnMetadata() {
            return this.right
        }

        IdentifyMetadataValues getIdentifyMetadataValues() {
            return this.identifyMetadataValues
        }

        IgnoreMetadataKeys getIgnoreMetadataKeys() {
            return this.ignoreMetadataKeys
        }
    }
}
