<#-- report/MaterialListBasicReporter/MainTemplate.ftl -->
<#import "../../filesystem/metadata/QueryOnMetadata/macros.ftl" as qom>
<#import "macros.ftl" as macros>

<!doctype html>
<html lang='en'>
  <head>
    <meta charset='utf-8' />
    <meta name='viewport' content='width=device-width, initial-scale=1' /><!-- Bootstrap -->
    <link href='https://cdn.jsdelivr.net/npm/bootstrap@5.1.0/dist/css/bootstrap.min.css' rel='stylesheet' integrity='sha384-KyZXEAg3QhqLMpG8r+8fhAXLRk2vvoC2f3B09zVXn8CA5QIVfZOJ3BCsw2P0p/We' crossorigin='anonymous' />
    <style>${style}</style>
    <title>${jobName.toString()}</title>
  </head>
  <body>
    <div class='container'>
      <h1 class='title'>${title}
        <button class='btn btn-secondary' type='button' data-bs-toggle='collapse' data-bs-target='#collapsingHeader' aria-expanded='false' aria-controls='collapsingHeader'>About</button>
      </h1>
      <div id='collapsingHeader' class='collapse header'>
        <dl>
          <dt>Root path :</dt>
          <dd>${store.getRoot().normalize().toString()}</dd>
          <dt>JobName :</dt>
          <dd>${jobName.toString()}</dd>
          <dt>MaterialList specification</dt>
          <dd>
            <dl>
              <dt>JobTimestamp :</dt>
              <dd>${model.jobTimestamp}</dd>
              <dt>QueryOnMetadata :</dt>
              <dd>
                <@qom.toSpanSequence keyValuePairs=model.queryOnMetadata />
              </dd>
            </dl>
          </dd>
        </dl>
      </div>
      <div class='accordion' id='accordionExample'>
        <#list model.materialList>
          <#items as material>
            <div class='accordion-item'>
              <h2 class='accordion-header' id='heading${material?counter}'>
                <button class='accordion-button' type='button' data-bs-toggle='collapse' data-bs-target='#collapse${material?counter}' area-expanded='false' aria-controls='collapse${material?counter}'>
                  <span class='fileType'>${material.fileType}</span>
                  <span class='metadata'>${material.metadataText}</span>
                </button>
              </h2>
              <div id='collapse${material?counter}' class='accordion-collapse collapse' aria-labelledby='heading${material?counter}' data-bs-parent='#accordionExample'>
                <div class='accordion-body'>
                  <@macros.makeAccordionBody material=material/>
                </div>
              </div>
            </div>
          </#items>
        </#list>
      </div>
    </div>
  </body>
</html>




