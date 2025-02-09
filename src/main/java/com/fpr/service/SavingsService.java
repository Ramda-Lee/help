package com.fpr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fpr.domain.SavingsProduct;
import com.fpr.dto.savings.SavingsRequestDto;
import com.fpr.dto.savings.SavingsResponseDto;
import com.fpr.persistence.SavingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SavingsService {

    private final SavingsRepository savingsRepository;

    public List<SavingsProduct> list(){
        return savingsRepository.findAll();
    }

    public void savingsApiSave() throws JsonProcessingException {

        HashMap<String, Object> result = new HashMap<>();

        try {
            RestTemplate rt = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = "http://finlife.fss.or.kr/finlifeapi/depositProductsSearch.json";

            UriComponents uri = UriComponentsBuilder.fromHttpUrl(url + "?" + "auth=41a94be07dfa9f03716566379d2d2371" + "&" + "topFinGrpNo=020000&pageNo=1").build();

            ResponseEntity<SavingsResponseDto> responseEntity = rt.exchange(uri.toString(), HttpMethod.GET, entity, SavingsResponseDto.class);
            HttpStatus statusCode = HttpStatus.valueOf(responseEntity.getStatusCodeValue());
            HttpHeaders header = responseEntity.getHeaders();
            SavingsResponseDto body = responseEntity.getBody();

            SavingsProduct savingsProduct = new SavingsProduct();

            List<SavingsRequestDto.OptionList> optionLists = body.getResult().getOptionList();

            List<SavingsProduct> products = body.getResult().getBaseList()
                    .stream()
                    .map(SavingsRequestDto.BaseList::toEntity)
                    .collect(Collectors.toList());



            optionLists.forEach(optionList -> {
                savingsProduct.setIntrRateType(optionList.getIntrRateType());
                savingsProduct.setIntrRateTypeNm(optionList.getIntrRateTypeNm());
                savingsProduct.setSaveTrm(optionList.getSaveTrm());
                savingsProduct.setIntrRate(optionList.getIntrRate());
                savingsProduct.setIntrRate2(optionList.getIntrRate2());

            });

            savingsRepository.saveAll(products);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            result.put("statusCode", e.getRawStatusCode());
            result.put("body", e.getStatusText());
            System.out.println(e.toString());

        } catch (Exception e) {
            result.put("statusCode", "999");
            result.put("body", "excpetion오류");
            System.out.println(e.toString());
        }


    }

    @Transactional(readOnly = true)
    public SavingsProduct findOne(Long sProductId) {
        Optional<SavingsProduct> product = savingsRepository.findById(sProductId);
        return product.orElseGet(() -> new SavingsProduct());
    }

//    public void recommendProduct(Member member) {
//        productRepository.recommend(member.getAge(), member.getJob());
//    }

    public List<SavingsProduct> searchProduct(SavingsProduct savingsProduct) {
        return savingsRepository.findBykorCoNm(savingsProduct.getKorCoNm());
    }

}
