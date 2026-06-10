package com.bn.atmcontratos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class NumeroLetras {
 private static final String[] UNIDADES={"","UN ","DOS ","TRES ","CUATRO ","CINCO ","SEIS ","SIETE ","OCHO ","NUEVE ","DIEZ ","ONCE ","DOCE ","TRECE ","CATORCE ","QUINCE ","DIECISÉIS ","DIECISIETE ","DIECIOCHO ","DIECINUEVE ","VEINTE ","VEINTIÚN ","VEINTIDÓS ","VEINTITRÉS ","VEINTICUATRO ","VEINTICINCO ","VEINTISÉIS ","VEINTISIETE ","VEINTIOCHO ","VEINTINUEVE "};
 private static final String[] DECENAS={"","DIEZ ","VEINTE ","TREINTA ","CUARENTA ","CINCUENTA ","SESENTA ","SETENTA ","OCHENTA ","NOVENTA "};
 private static final String[] CENTENAS={"","CIENTO ","DOSCIENTOS ","TRESCIENTOS ","CUATROCIENTOS ","QUINIENTOS ","SEISCIENTOS ","SETECIENTOS ","OCHOCIENTOS ","NOVECIENTOS "};

 public static String moneda(BigDecimal monto, String moneda){
  if(monto==null) monto=BigDecimal.ZERO;
  BigDecimal v=monto.setScale(2, RoundingMode.HALF_UP);
  long entero=v.longValue();
  int decimales=v.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
  String monedaTexto=normalizarMoneda(moneda);
  return numeroALetras(entero).trim()+" CON "+String.format(Locale.ROOT,"%02d",decimales)+"/100 "+monedaTexto;
 }

 private static String normalizarMoneda(String m){
  String s=m==null?"":m.toLowerCase(Locale.ROOT);
  if(s.contains("dólar")||s.contains("dolar")||s.contains("us")) return "DÓLARES AMERICANOS";
  return "SOLES";
 }

 private static String numeroALetras(long numero){
  if(numero==0) return "CERO ";
  if(numero<0) return "MENOS "+numeroALetras(Math.abs(numero));
  if(numero>999_999_999) return "NÚMERO MUY GRANDE ";
  if(numero<30) return UNIDADES[(int)numero];
  if(numero<100) return DECENAS[(int)(numero/10)]+(numero%10!=0?"Y "+UNIDADES[(int)(numero%10)]:"");
  if(numero==100) return "CIEN ";
  if(numero<1000) return CENTENAS[(int)(numero/100)]+numeroALetras(numero%100);
  if(numero==1000) return "MIL ";
  if(numero<1_000_000){
   String miles=(numero/1000)==1?"MIL ":numeroALetras(numero/1000)+"MIL ";
   return miles+numeroALetras(numero%1000);
  }
  if(numero==1_000_000) return "UN MILLÓN ";
  String millones=(numero/1_000_000)==1?"UN MILLÓN ":numeroALetras(numero/1_000_000)+"MILLONES ";
  return millones+numeroALetras(numero%1_000_000);
 }
}
