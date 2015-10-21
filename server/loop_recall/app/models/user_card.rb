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

class UserCard < ActiveRecord::Base
  belongs_to :user
  belongs_to :card
end
