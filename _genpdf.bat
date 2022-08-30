cd output/
wkhtmltopdf --enable-local-file-access toc.html 01toc.pdf
wkhtmltopdf --enable-local-file-access index.html 02index.pdf
wkhtmltopdf --enable-local-file-access protocol_overview.html 03protocol_overview.pdf
wkhtmltopdf --enable-local-file-access architecture.html 04architecture.pdf
wkhtmltopdf --enable-local-file-access artifacts.html 05artifacts.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-Group-Cohort-IEHR.html 06cohort.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-Group-Cohort-IEHR-definitions.html 07cohortdefinintion.pdf
wkhtmltopdf --enable-local-file-access Group-exampleGroupCohort1.html 08cohortexample.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-ResearchStudy-ResearchStudy-IEHR.html 09researchstudy.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-ResearchStudy-ResearchStudy-IEHR-definitions.html 10researchstudydefinintion.pdf
wkhtmltopdf --enable-local-file-access ResearchStudy-exampleStudy_extern.html 11researchstudyexample.pdf
wkhtmltopdf --enable-local-file-access Bundle-ResearchStudyBundle.html 12researchstudyexamplebundle.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-Location-ReferenceResearchCenter-IEHR.html 13researchlocation.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-Location-ReferenceResearchCenter-IEHR-definitions.html 14researchlocationdefinition.pdf
wkhtmltopdf --enable-local-file-access Location-exampleLocation1.html 15researchlocationexample.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-DataSetDefinition.html 16datasetdefinition.pdf
wkhtmltopdf --enable-local-file-access StructureDefinition-DataSetDefinition-definitions.html 17datasetdefinitiondefinition.pdf
wkhtmltopdf --enable-local-file-access Endpoint-exampleEndpoint.html 18endpoint.pdf
del /f rds-ig.pdf
pdftk *.pdf cat output rds-ig.pdf
del /f 01toc.pdf,02index.pdf,03protocol_overview.pdf,04architecture.pdf,05artifacts.pdf,06cohort.pdf,07cohortdefinintion.pdf,08cohortexample.pdf,09researchstudy.pdf,10researchstudydefinintion.pdf,11researchstudyexample.pdf,12researchstudyexamplebundle.pdf,13researchlocation.pdf,14researchlocationdefinition.pdf,15researchlocationexample.pdf,16datasetdefinition.pdf,17datasetdefinitiondefinition.pdf,18endpoint.pdf
PAUSE
