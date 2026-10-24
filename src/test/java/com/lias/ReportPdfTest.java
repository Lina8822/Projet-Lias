package com.lias;
import com.lias.service.ReportPdf;
import com.lias.view.Page;
import java.nio.charset.Charset;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportPdfTest {
 @Test void frenchAccentsAreEncodedWithDeclaredWinAnsiFont()throws Exception {
  Page p=new Page("rapport","é è ê à â ù ç É À Ç");
  byte[] bytes=ReportPdf.render(p);
  String pdf=new String(bytes,Charset.forName("windows-1252"));
  assertTrue(pdf.startsWith("%PDF-1.4"));assertTrue(pdf.contains("/WinAnsiEncoding"));
  assertTrue(pdf.contains("(é è ê à â ù ç É À Ç)"));
  java.nio.file.Files.createDirectories(java.nio.file.Path.of("build/current-verification"));
  java.nio.file.Files.write(java.nio.file.Path.of("build/current-verification/accents.pdf"),bytes);
 }
 @Test void longTablesPaginateAndEscapePdfSyntax()throws Exception {
  Page p=new Page("rapport","Rapport (LIAS) \\ annuel");
  List<Page.Row> rows=new ArrayList<>();
  for(int i=0;i<200;i++)rows.add(new Page.Row(Map.of("titre","Publication "+i),List.of()));
  p.blocks.add(new Page.Block("Production",Map.of("titre","Titre"),rows));
  String pdf=new String(ReportPdf.render(p),Charset.forName("windows-1252"));
  assertTrue(pdf.contains("Production \\(suite\\)"));
  assertTrue(pdf.contains("Publication 199"));
  assertTrue(pdf.contains("Rapport \\(LIAS\\) \\\\ annuel"));
 }
}
