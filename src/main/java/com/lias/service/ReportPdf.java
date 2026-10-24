package com.lias.service;

import com.lias.view.Page;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/** Small PDF 1.4 writer for paginated French tabular reports; no external library. */
public final class ReportPdf {
 private static final Charset ENCODING=Charset.forName("windows-1252");
 private final List<StringBuilder> pages=new ArrayList<>();
 private StringBuilder content; private int y;
 private String tableTitle; private List<String> tableHeaders; private boolean header;
 private void page(){content=new StringBuilder();pages.add(content);y=790;text(42,815,10,"LIAS - Rapport d'activité annuel");text(42,24,9,"LIAS - Page "+pages.size());}
 private void text(int x,int baseline,int size,String value){content.append("BT /F1 ").append(size).append(" Tf ").append(x).append(' ').append(baseline).append(" Td (").append(escape(value)).append(") Tj ET\n");}
 private static String escape(String value){return value.replace("\\","\\\\").replace("(","\\(").replace(")","\\)").replaceAll("[\\r\\n\\t]"," ");}
 private void line(String text,int size){if(y<60)page();text(42,y,size,text);y-=size+7;}
 private static List<String> wrap(String value,int width){
  List<String> lines=new ArrayList<>();String remaining=value.replaceAll("\\s+"," ").trim();
  while(remaining.length()>width){int cut=remaining.lastIndexOf(' ',width);if(cut<width/2)cut=width;lines.add(remaining.substring(0,cut));remaining=remaining.substring(cut).stripLeading();}
  lines.add(remaining);return lines;
 }
 private void row(List<String> values){
  int n=values.size(),width=510/n,chars=Math.max(8,width/6-2);
  List<List<String>> cells=values.stream().map(v->wrap(v,chars)).toList();int count=cells.stream().mapToInt(List::size).max().orElse(1);
  if(y-count*13-7<65 && count<45 && !header){tablePage();}
  for(int i=0;i<count;i++){
   if(y<65)tablePage();
   for(int j=0;j<n;j++)if(i<cells.get(j).size())text(44+j*width,y,9,cells.get(j).get(i));
   y-=13;
  }
  content.append("0.8 G 42 ").append(y+5).append(" m 552 ").append(y+5).append(" l S 0 G\n");y-=7;
 }
 private void tablePage(){page();line(tableTitle+" (suite)",11);header=true;row(tableHeaders);header=false;}
 public static byte[] render(Page report)throws IOException {
  ReportPdf pdf=new ReportPdf();pdf.page();pdf.line(report.title,16);pdf.line(report.subtitle,9);
  for(var stat:report.stats.entrySet())pdf.line(stat.getKey()+" : "+stat.getValue(),10);
  for(var block:report.blocks){
   if(pdf.y<180)pdf.page();pdf.y-=10;pdf.line(block.title(),11);
   if(block.rows().isEmpty()){pdf.line("Aucune donnée pour cette période.",9);continue;}
   pdf.tableTitle=block.title();pdf.tableHeaders=new ArrayList<>(block.columns().values());pdf.header=true;pdf.row(pdf.tableHeaders);pdf.header=false;
   for(var row:block.rows())pdf.row(block.columns().keySet().stream().map(k->Objects.toString(row.values().get(k),"-").replaceAll("(\\d{4}-\\d{2}-\\d{2})T","$1 ")).toList());
  }
  return pdf.bytes();
 }
 private byte[] bytes()throws IOException {
  List<byte[]> objects=new ArrayList<>();objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
  StringBuilder kids=new StringBuilder();for(int i=0;i<pages.size();i++)kids.append(4+2*i).append(" 0 R ");
  objects.add(bytes("<< /Type /Pages /Count "+pages.size()+" /Kids ["+kids+"] >>"));
  objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
  for(int i=0;i<pages.size();i++){
   objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents "+(5+2*i)+" 0 R >>"));
   byte[] stream=bytes(pages.get(i).toString());ByteArrayOutputStream obj=new ByteArrayOutputStream();obj.write(bytes("<< /Length "+stream.length+" >>\nstream\n"));obj.write(stream);obj.write(bytes("endstream"));objects.add(obj.toByteArray());
  }
  ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(bytes("%PDF-1.4\n"));List<Integer> offsets=new ArrayList<>();
  for(int i=0;i<objects.size();i++){offsets.add(out.size());out.write(bytes((i+1)+" 0 obj\n"));out.write(objects.get(i));out.write(bytes("\nendobj\n"));}
  int xref=out.size();out.write(bytes("xref\n0 "+(objects.size()+1)+"\n0000000000 65535 f \n"));
  for(int offset:offsets)out.write(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",offset)));
  out.write(bytes("trailer\n<< /Size "+(objects.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));return out.toByteArray();
 }
 private static byte[] bytes(String s){return s.getBytes(ENCODING);}
}
