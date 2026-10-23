package com.shortener.domain.service;

import com.shortener.domain.model.ShortCode;

/*Domain service for generating short codes.*/

public interface ShortCodeGenerator {
    ShortCode generate();
}
