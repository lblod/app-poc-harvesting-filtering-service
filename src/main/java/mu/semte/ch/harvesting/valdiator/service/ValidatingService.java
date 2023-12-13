package mu.semte.ch.harvesting.valdiator.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.lib.dto.DataContainer;
import mu.semte.ch.lib.dto.Task;
import mu.semte.ch.lib.shacl.ShaclService;
import mu.semte.ch.lib.utils.ModelUtils;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValidatingService {
                private final ShaclService shaclService;
                private final TaskService taskService;
                @SuppressWarnings("unused")
                private final XlsReportService xlsReportService;
                @Value("${sparql.defaultLimitSize}")
                private int defaultLimitSize;

                public ValidatingService(ShaclService shaclService, TaskService taskService,
                                                XlsReportService xlsReportService) {
                                this.shaclService = shaclService;
                                this.taskService = taskService;
                                this.xlsReportService = xlsReportService;
                }

                public void runValidatePipeline(Task task) {
                                var inputContainer = taskService.selectInputContainer(task).get(0);
                                var countTriples = taskService.countTriplesFromFileInputContainer(
                                                                inputContainer.getGraphUri());
                                var pagesCount = countTriples > defaultLimitSize ? countTriples / defaultLimitSize : 1;

                                var fileContainer = DataContainer.builder().build();
                                var resultContainer = DataContainer.builder()
                                                                .graphUri(inputContainer.getGraphUri())
                                                                .validationGraphUri(fileContainer.getUri())
                                                                .build();

                                for (var i = 0; i <= pagesCount; i++) {
                                                var offset = i * defaultLimitSize;
                                                var importedTriples = taskService.fetchTripleFromFileInputContainer(
                                                                                inputContainer.getGraphUri(),
                                                                                defaultLimitSize, offset);
                                                for (var mbd : importedTriples) {
                                                                log.info("writing report for {}", mbd.derivedFrom());
                                                                writeValidationReport(task, fileContainer, mbd);
                                                                // var _reportGraph = report.getKey().getGraphUri();
                                                                // xlsReportService.writeReport(task, report.getValue(),
                                                                // fileContainer,
                                                                // mbd.derivedFrom());
                                                                // var dataContainer = DataContainer.builder()
                                                                // .graphUri(reportGraph)
                                                                // .build();
                                                                // taskService.appendTaskResultFile(task,
                                                                // dataContainer);
                                                }
                                }

                                taskService.appendTaskResultGraph(task, resultContainer);
                }

                private Map.Entry<DataContainer, Model> writeValidationReport(Task task, DataContainer fileContainer,
                                                ModelByDerived importedTriples) {
                                log.debug("generate validation reports...");
                                var report = shaclService.validate(importedTriples.model().getGraph());
                                log.debug("triples conforms: {}", report.conforms());
                                var reportModel = ModelUtils.replaceAnonNodes(report.getModel());
                                var dataContainer = fileContainer.toBuilder()
                                                                .graphUri(taskService.writeTtlFile(
                                                                                                task.getGraph(),
                                                                                                new ModelByDerived(importedTriples
                                                                                                                                .derivedFrom(),
                                                                                                                                reportModel),
                                                                                                "validation-report.ttl"))
                                                                .build();
                                taskService.appendTaskResultFile(task, dataContainer);
                                return Map.entry(dataContainer, reportModel);
                }
}
