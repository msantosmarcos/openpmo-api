package br.gov.es.openpmo.utils;

import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.reports.ReportFormat;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.*;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class JasperUtils {

  public static OutputStream compileJrxml(File file) throws IOException, JRException {
    final OutputStream outputStream = new ByteArrayOutputStream();
    final InputStream inputStream = Files.newInputStream(file.toPath());
    JasperCompileManager.compileReportToStream(inputStream, outputStream);
    return outputStream;
  }

  public static JasperReport getJasperReportFromJasperFile(File file) throws IOException, ClassNotFoundException {
    final InputStream inputStream = Files.newInputStream(file.toPath());
    try (ObjectInputStream oin = new ObjectInputStream(inputStream)) {
      return (JasperReport) oin.readObject();
    }
  }

  public byte[] print(Map<String, Object> params, List<?> lista, ReportFormat tipo, JasperReport jasperReport) {
    JRDataSource dataSource = this.getDataSource(lista);
    return this.print(params, dataSource, tipo, jasperReport);
  }

  public byte[] print(Map<String, Object> params, JRDataSource dataSource, ReportFormat tipo, JasperReport jasperReport) {

    byte[] report;

    try {

      final ByteArrayOutputStream out = new ByteArrayOutputStream();

      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

      switch (tipo) {
        case PDF:
          report = JasperExportManager.exportReportToPdf(jasperPrint);
          break;
        case HTML:
          HtmlExporter exporterHtml = new HtmlExporter();
          exporterHtml.setExporterInput(new SimpleExporterInput(jasperPrint));
          Map<String, String> images = new HashMap<>();
          SimpleHtmlExporterOutput simpleHtmlExporterOutput = new SimpleHtmlExporterOutput(out);
          simpleHtmlExporterOutput.setImageHandler(new HtmlResourceHandler() {

            @Override
            public String getResourcePath(String id) {
              return images.get(id);
            }

            @Override
            public void handleResource(String id, byte[] data) {
              if (id.endsWith("JPEG") || id.endsWith("jpeg"))
                images.put(id, "data:image/jpeg;base64," + Arrays.toString(Base64.getEncoder().encode(data)));
              if (id.endsWith("JPG") || id.endsWith("jpg"))
                images.put(id, "data:image/jpg;base64," + Arrays.toString(Base64.getEncoder().encode(data)));
              if (id.endsWith("PNG") || id.endsWith("png"))
                images.put(id, "data:image/jpg;base64," + Arrays.toString(Base64.getEncoder().encode(data)));
              if (id.endsWith("SVG") || id.endsWith("svg"))
                images.put(id, "data:image/svg+xml;base64," + Arrays.toString(Base64.getEncoder().encode(data)));
            }
          });
          exporterHtml.setExporterOutput(simpleHtmlExporterOutput);
          exporterHtml.exportReport();
          report = out.toByteArray();
          break;
        case ODT:
          JRDocxExporter exporter = new JRDocxExporter();
          exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
          exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
          exporter.exportReport();
          report = out.toByteArray();
          break;
        case XLS:
          JRXlsxExporter exporterXlsx = new JRXlsxExporter();
          exporterXlsx.setExporterInput(new SimpleExporterInput(jasperPrint));

          SimpleXlsxReportConfiguration xlsReportConfiguration = new SimpleXlsxReportConfiguration();
          xlsReportConfiguration.setOnePagePerSheet(false);
          xlsReportConfiguration.setRemoveEmptySpaceBetweenRows(true);
          xlsReportConfiguration.setDetectCellType(false);
          xlsReportConfiguration.setWhitePageBackground(false);
          exporterXlsx.setConfiguration(xlsReportConfiguration);

          SimpleOutputStreamExporterOutput simpleOutputStreamExporterOutput = new SimpleOutputStreamExporterOutput(out);
          exporterXlsx.setExporterOutput(simpleOutputStreamExporterOutput);
          exporterXlsx.exportReport();
          report = out.toByteArray();
          break;
        case CSV:
          JRCsvExporter exporterCSV = new JRCsvExporter();
          exporterCSV.setExporterInput(new SimpleExporterInput(jasperPrint));

          SimpleHtmlExporterOutput simpleHtmlExporterOutputToCSV = new SimpleHtmlExporterOutput(out);
          exporterCSV.setExporterOutput(simpleHtmlExporterOutputToCSV);
          exporterCSV.exportReport();
          report = out.toByteArray();
          break;
        case TXT:
          JRTextExporter exporterTxt = new JRTextExporter();
          exporterTxt.setExporterInput(new SimpleExporterInput(jasperPrint));
          exporterTxt.setExporterOutput(new SimpleWriterExporterOutput(out));
          // https://jasperreports.sourceforge.net/sample.reference/text/index.html
          final SimpleTextReportConfiguration configuration = new SimpleTextReportConfiguration();
          configuration.setCharHeight(13.948f);
          configuration.setCharWidth(7.238f);
          exporterTxt.setConfiguration(configuration);
          exporterTxt.exportReport();
          report = out.toByteArray();
          break;
        case RTF:
          JRRtfExporter exportRtf = new JRRtfExporter();
          exportRtf.setExporterInput(new SimpleExporterInput(jasperPrint));
          exportRtf.setExporterOutput(new SimpleWriterExporterOutput(out));
          exportRtf.exportReport();
          report = out.toByteArray();
          break;
        case XML:
          JRXmlExporter exportXml = new JRXmlExporter();
          exportXml.setExporterInput(new SimpleExporterInput(jasperPrint));
          exportXml.setExporterOutput(new SimpleXmlExporterOutput(out));
          exportXml.exportReport();
          report = out.toByteArray();
          break;
        default:
          throw new NegocioException(ApplicationMessage.REPORT_GENERATE_UNKNOWN_TYPE_ERROR);
      }

    } catch (JRException e) {
      e.printStackTrace();
      throw new NegocioException(ApplicationMessage.REPORT_GENERATE_ERROR);
    }

    return report;
  }

  private JRDataSource getDataSource(List<?> lista) {
    if (lista == null || lista.isEmpty())
      return new JREmptyDataSource();
    else {
      return new JRBeanCollectionDataSource(lista);
    }
  }

}
