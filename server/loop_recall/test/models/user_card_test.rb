# == Schema Information
#
# Table name: user_cards
#
#  id         :integer          not null, primary key
#  user_id    :integer
#  card_id    :integer
#  created_at :datetime         not null
#  updated_at :datetime         not null
#

require 'test_helper'

class UserCardTest < ActiveSupport::TestCase
  # test "the truth" do
  #   assert true
  # end
end
