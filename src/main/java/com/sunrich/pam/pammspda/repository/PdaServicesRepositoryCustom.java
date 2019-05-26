package com.sunrich.pam.pammspda.repository;

import java.util.List;

public interface PdaServicesRepositoryCustom {
  Integer getGroup(Long pdaId, String status);

  Integer getMaxGroup(Long pdaId);
}
