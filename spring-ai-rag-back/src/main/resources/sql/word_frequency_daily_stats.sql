ALTER TABLE `word_frequency`
  ADD KEY `idx_word_frequency_day` (`word`, `business_type`, `create_time`),
  ADD KEY `idx_word_frequency_create_time` (`create_time`);
