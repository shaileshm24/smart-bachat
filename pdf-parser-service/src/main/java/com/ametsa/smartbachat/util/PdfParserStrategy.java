package com.ametsa.smartbachat.util;


import com.ametsa.smartbachat.entity.TransactionEntity;
import java.util.List;

public interface PdfParserStrategy {
    List<TransactionEntity> parse(String pageText, Long openingBalancePaisa);
}
