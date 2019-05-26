package com.sunrich.pam.pammspda.repository;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.pda.PdaServices;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@Repository
public class PdaServicesRepositoryImpl implements PdaServicesRepositoryCustom {
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Integer getGroup(Long pdaId, String status) {
    Session session = (Session) entityManager.getDelegate();
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
    Root<PdaServices> pdaServicesRoot = criteriaQuery.from(PdaServices.class);
    criteriaQuery.select(pdaServicesRoot.get("serviceGroup"));
    criteriaQuery.where(criteriaBuilder.equal(pdaServicesRoot.get("pdaId"), pdaId),
            criteriaBuilder.equal(pdaServicesRoot.get("status"), status),
            criteriaBuilder.equal(pdaServicesRoot.get("recordStatus"), true),
            criteriaBuilder.equal(pdaServicesRoot.get("type"), Constants.PdaType.SPDA));
    criteriaQuery.groupBy(pdaServicesRoot.get("pdaId"));
    Query<Integer> query = session.createQuery(criteriaQuery);

    return query.uniqueResult();
  }

  @Override
  public Integer getMaxGroup(Long pdaId) {
    Session session = (Session) entityManager.getDelegate();
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
    Root<PdaServices> pdaServicesRoot = criteriaQuery.from(PdaServices.class);
    criteriaQuery.select(criteriaBuilder.max(pdaServicesRoot.get("serviceGroup")));
    criteriaQuery.where(criteriaBuilder.equal(pdaServicesRoot.get("pdaId"), pdaId),
            criteriaBuilder.equal(pdaServicesRoot.get("recordStatus"), true),
            criteriaBuilder.equal(pdaServicesRoot.get("type"), Constants.PdaType.SPDA));
    criteriaQuery.groupBy(pdaServicesRoot.get("pdaId"));
    Query<Integer> query = session.createQuery(criteriaQuery);
    return query.uniqueResult();
  }

}
