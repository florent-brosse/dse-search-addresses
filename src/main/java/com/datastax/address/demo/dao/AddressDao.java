package com.datastax.address.demo.dao;

import com.datastax.address.demo.model.Address;
import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.dse.driver.api.core.cql.reactive.ReactiveResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;

@Repository
public class AddressDao {

    @Autowired
    private DseSession dseSession;

    @Autowired
    private RowToAddressMapper rowMapper;


    private PreparedStatement solrPreparedStatement;

    @PostConstruct
    public void init() {
        solrPreparedStatement = dseSession.prepare("SELECT * from address where solr_query=? limit 10");
    }

    public Flux<Address> search(String address) {

        String query = makeSearch(address.replace("\""," "));
        System.out.println(query);
        BoundStatement searchStatement = solrPreparedStatement.bind(query).setIdempotent(true);
        ReactiveResultSet rs = dseSession.executeReactive(searchStatement);
        return Flux.from(rs).map(rowMapper);
    }

    private String makeSearch(String address) {
        var strings = Arrays.stream(address.split("\\s+")).map(s -> {
            if (s.matches("\\d+")) return s;
            else return s + "~";
        }).collect(Collectors.toList());
        return "{\"q\":\"full_address:(" + String.join(" AND ", strings) + "*)\"}";
    }
}
